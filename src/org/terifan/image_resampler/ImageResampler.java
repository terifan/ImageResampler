package org.terifan.image_resampler;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import org.terifan.image_resampler.FilterFactory.Filter;


/**
 * Methods for slow but high quality image resizing.
 */
public class ImageResampler
{
	public static BufferedImage getScaledImageAspect(BufferedImage aSource, int aWidth, int aHeight, boolean aSRGB, Filter aFilter)
	{
		return getScaledImageAspectImpl(aSource, aWidth, aHeight, aSRGB, aFilter, false);
	}


	public static BufferedImage getScaledImageAspectOuter(BufferedImage aSource, int aWidth, int aHeight, boolean aSRGB, Filter aFilter)
	{
		return getScaledImageAspectImpl(aSource, aWidth, aHeight, aSRGB, aFilter, true);
	}


	private static BufferedImage getScaledImageAspectImpl(BufferedImage aSource, int aWidth, int aHeight, boolean aSRGB, Filter aFilter, boolean aOuter)
	{
		Dimension dim = getScaledImageAspectSize(new Dimension(aSource.getWidth(), aSource.getHeight()), aWidth, aHeight, aOuter);

		if (dim.width < 1 || dim.height < 1)
		{
			return aSource;
		}

		// make sure one direction has specified dimension
		if (dim.width != aWidth && dim.height != aHeight)
		{
			if (Math.abs(aWidth - dim.width) < Math.abs(aHeight - dim.height))
			{
				dim.width = aWidth;
			}
			else
			{
				dim.height = aHeight;
			}
		}

		return getScaledImage(aSource, dim.width, dim.height, aSRGB, aFilter);
	}


	public static BufferedImage getScaledImage(BufferedImage aSource, int aWidth, int aHeight, boolean aSRGB, Filter aFilter)
	{
		if (aWidth < aSource.getWidth() || aHeight < aSource.getHeight())
		{
			aSource = resizeDown(aSource, aWidth, aHeight, aSRGB, aFilter);
		}
		if (aWidth > aSource.getWidth() || aHeight > aSource.getHeight())
		{
			aSource = resizeUp(aSource, aWidth, aHeight, aSRGB);
		}

		return aSource;
	}


	private static BufferedImage resizeUp(BufferedImage aSource, int aWidth, int aHeight, boolean aQuality)
	{
		return resizeUpImpl(aSource, aWidth, aHeight, aQuality);
	}


	private static BufferedImage resizeUpImpl(BufferedImage aSource, int aWidth, int aHeight, boolean aQuality)
	{
		BufferedImage output = new BufferedImage(aWidth, aHeight, aSource.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = output.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, aQuality ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(aSource, 0, 0, aWidth, aHeight, null);
		g.dispose();

		return output;
	}


	public static BufferedImage resizeDown(BufferedImage aImage, int aDstWidth, int aDstHeight, boolean aSRGB, Filter aFilter)
	{
		int srcWidth = aImage.getWidth();
		int srcHeight = aImage.getHeight();

		double[][][] pixels = new double[srcHeight][srcWidth][3];

		if (aSRGB)
		{
			fromSRGB(aImage, pixels);
		}
		else
		{
			fromRGB(aImage, pixels);
		}

		pixels = transpose(resample(pixels, srcWidth, srcHeight, aDstWidth, aFilter));
		pixels = transpose(resample(pixels, srcHeight, aDstWidth, aDstHeight, aFilter));

		BufferedImage outImage = new BufferedImage(aDstWidth, aDstHeight, BufferedImage.TYPE_INT_RGB);

		if (aSRGB)
		{
			toSRGB(outImage, pixels);
		}
		else
		{
			toRGB(outImage, pixels);
		}

		return outImage;
	}


	private static double[][][] resample(double[][][] aInput, int aSrcWidth, int aSrcHeight, int aNewWidth, Filter aKernel)
	{
		double[][][] output = new double[aSrcHeight][aNewWidth][3];
		double filterLen = Math.max(aSrcWidth / (double)aNewWidth, 1) * aKernel.getRadius() * 2;
		double scale = Math.min(aNewWidth / (double)aSrcWidth, 1);

		Parallel.rangeClosed(0, aSrcHeight, 10).forEach((y0, y1) ->
		{
			for (int y = y0; y < y1; y++)
			{
				for (int x = 0; x < aNewWidth; x++)
				{
					double centerX = (0.5 + x) / aNewWidth * aSrcWidth;
					double filterStartX = centerX - filterLen / 2.0;
					int inputX = (int)Math.ceil(filterStartX - 0.5);

					double r = 0;
					double g = 0;
					double b = 0;
					double w = 0;

					for (int f = 0; f < filterLen; f++, inputX++)
					{
						double xi = (inputX + 0.5 - centerX) * scale;
						double yi = (inputX + 0.5 - filterStartX) / filterLen;

						double k = yi >= 0 && yi <= 1 ? aKernel.filter(xi) * (1 - Math.abs(yi - 0.5) * 2) : 0;
						int q = Math.min(Math.max(inputX, 0), aSrcWidth - 1);

						double[] c = aInput[y][q];
						r += k * c[0];
						g += k * c[1];
						b += k * c[2];
						w += k;
					}

					double iw = w == 0 ? 0 : 1 / w;
					output[y][x][0] = r * iw;
					output[y][x][1] = g * iw;
					output[y][x][2] = b * iw;
				}
			}
		});

		return output;
	}


	private static double[][][] transpose(double[][][] aInput)
	{
		double[][][] output = new double[aInput[0].length][aInput.length][3];

		Parallel.range(0, aInput.length).forEach(y ->
		{
			for (int x = 0; x < aInput[0].length; x++)
			{
				output[x][y] = aInput[y][x];
			}
		});

		return output;
	}


	public static Dimension getScaledImageAspectSize(Dimension aSource, int aWidth, int aHeight, boolean aOuter)
	{
		double scale;
		if (aOuter)
		{
			scale = Math.max(aWidth / (double)aSource.width, aHeight / (double)aSource.height);
		}
		else
		{
			scale = Math.min(aWidth / (double)aSource.width, aHeight / (double)aSource.height);
		}

		aSource.width = (int)Math.round(aSource.width * scale);
		aSource.height = (int)Math.round(aSource.height * scale);

		return aSource;
	}

	private final static double GAMMA = 2.4;


	private static void toRGB(BufferedImage aImage, double[][][] aColors)
	{
		Parallel.range(0, aImage.getHeight()).forEach(y ->
		{
			for (int x = 0, w = aImage.getWidth(); x < w; x++)
			{
				double[] c = aColors[y][x];
				int r = mul8(c[0]) << 16;
				int g = mul8(c[1]) << 8;
				int b = mul8(c[2]);
				aImage.setRGB(x, y, 0xff000000 | r + g + b);
			}
		});
	}


	private static void toSRGB(BufferedImage aImage, double[][][] aColors)
	{
		Parallel.range(0, aImage.getHeight()).forEach(y ->
		{
			for (int x = 0, w = aImage.getWidth(); x < w; x++)
			{
				double[] c = aColors[y][x];
				int r = mul8(gamme(c[0])) << 16;
				int g = mul8(gamme(c[1])) << 8;
				int b = mul8(gamme(c[2]));
				aImage.setRGB(x, y, 0xff000000 | r + g + b);
			}
		});
	}


	private static void fromRGB(BufferedImage aImage, double[][][] aColors)
	{
		Parallel.range(0, aImage.getHeight()).forEach(y ->
		{
			for (int x = 0, w = aImage.getWidth(); x < w; x++)
			{
				int c = aImage.getRGB(x, y);
				aColors[y][x][0] = (0xff & (c >> 16)) / 255f;
				aColors[y][x][1] = (0xff & (c >> 8)) / 255f;
				aColors[y][x][2] = (0xff & (c)) / 255f;
			}
		});
	}


	private static void fromSRGB(BufferedImage aImage, double[][][] aColors)
	{
		Parallel.range(0, aImage.getHeight()).forEach(y ->
		{
			for (int x = 0, w = aImage.getWidth(); x < w; x++)
			{
				int c = aImage.getRGB(x, y);
				aColors[y][x][0] = gamma_inv((0xff & (c >> 16)) / 255.0);
				aColors[y][x][1] = gamma_inv((0xff & (c >> 8)) / 255.0);
				aColors[y][x][2] = gamma_inv((0xff & (c)) / 255.0);
			}
		});
	}


	private static double gamme(double x)
	{
		if (x >= 0.0031308)
		{
			return (1.055) * Math.pow(x, 1.0 / GAMMA) - 0.055;
		}
		return 12.92 * x;
	}


	private static double gamma_inv(double x)
	{
		if (x >= 0.04045)
		{
			return Math.pow((x + 0.055) / (1 + 0.055), GAMMA);
		}
		return x / 12.92;
	}


	private static int mul8(double aValue)
	{
		return aValue <= 0 ? 0 : aValue > 1 - 0.5 / 255 ? 255 : (int)(255 * aValue + 0.5);
	}
}
