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

		for (int y = 0; y < srcHeight; y++)
		{
			for (int x = 0; x < srcWidth; x++)
			{
				if (aSRGB)
				{
					fromSRGB(aImage.getRGB(x, y), pixels[y][x]);
				}
				else
				{
					fromRGB(aImage.getRGB(x, y), pixels[y][x]);
				}
			}
		}

		pixels = transpose(resample(pixels, srcWidth, srcHeight, aDstWidth, aFilter));
		pixels = transpose(resample(pixels, srcHeight, aDstWidth, aDstHeight, aFilter));

		BufferedImage outImage = new BufferedImage(aDstWidth, aDstHeight, BufferedImage.TYPE_INT_RGB);

		for (int sy = 0; sy < aDstHeight; sy++)
		{
			for (int sx = 0; sx < aDstWidth; sx++)
			{
				double[] c = pixels[sy][sx];

				if (aSRGB)
				{
					outImage.setRGB(sx, sy, toSRGB(c));
				}
				else
				{
					outImage.setRGB(sx, sy, toRGB(c));
				}
			}
		}

		return outImage;
	}


	private static double[][][] resample(double[][][] aInput, int aSrcWidth, int aSrcHeight, int aNewWidth, Filter aKernel)
	{
		double[][][] output = new double[aSrcHeight][aNewWidth][3];
		System.out.println(aKernel.getRadius());
		try (FixedThreadExecutor executor = new FixedThreadExecutor(1f))
		{
//			double filterLen = Math.max(aSrcWidth / (double)aNewWidth, 1) * 4.0;
			double filterLen = Math.max(aSrcWidth / (double)aNewWidth, 1) * aKernel.getRadius() * 2;
			double scale = Math.min(aNewWidth / (double)aSrcWidth, 1);

			for (int y = 0; y < aSrcHeight; y++)
			{
				double[][] _input = aInput[y];
				double[][] _output = output[y];

				executor.submit(() ->
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

							if (yi >= 0 && yi <= 1)
							{
								double k = aKernel.filter(xi) * (1 - Math.abs(yi - 0.5) * 2);
								int q = Math.min(Math.max(inputX, 0), aSrcWidth - 1);

								double[] c = _input[q];
								r += k * c[0];
								g += k * c[1];
								b += k * c[2];
								w += k;
							}
						}

//						for (int f = 0; f < filterLen; f++, inputX++)
//						{
//							double xi = (inputX + 0.5 - centerX) * scale;
//							double yi = (inputX + 0.5 - filterStartX) / filterLen;
//
//							double k = aKernel.filter(xi) * aKernel.filter((yi - 0.5) * 2);
//							int q = Math.min(Math.max(inputX, 0), aSrcWidth - 1);
//
//							double[] c = _input[q];
//							r += k * c[0];
//							g += k * c[1];
//							b += k * c[2];
//							w += k;
//						}

						if (w > 0)
						{
							double iw = 1 / w;
							_output[x][0] = r * iw;
							_output[x][1] = g * iw;
							_output[x][2] = b * iw;
						}
					}
				});
			}
		}

		return output;
	}


	private static double[][][] transpose(double[][][] aInput)
	{
		double[][][] output = new double[aInput[0].length][aInput.length][3];

		for (int y = 0; y < aInput.length; y++)
		{
			for (int x = 0; x < aInput[0].length; x++)
			{
				output[x][y] = aInput[y][x];
			}
		}

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


	private static int toRGB(double[] aColor)
	{
		int r = mul8(aColor[0]) << 16;
		int g = mul8(aColor[1]) << 8;
		int b = mul8(aColor[2]);

		return 0xff000000 | r + g + b;
	}


	private static int toSRGB(double[] aColor)
	{
		int r = mul8(f(aColor[0])) << 16;
		int g = mul8(f(aColor[1])) << 8;
		int b = mul8(f(aColor[2]));

		return 0xff000000 | r + g + b;
	}


	private static void fromRGB(int aColor, double[] aDest)
	{
		aDest[0] = (0xff & (aColor >> 16)) / 255f;
		aDest[1] = (0xff & (aColor >>  8)) / 255f;
		aDest[2] = (0xff & (aColor      )) / 255f;
	}


	private static void fromSRGB(int aColor, double[] aDest)
	{
		aDest[0] = f_inv((0xff & (aColor >> 16)) / 255.0);
		aDest[1] = f_inv((0xff & (aColor >> 8)) / 255.0);
		aDest[2] = f_inv((0xff & (aColor)) / 255.0);
	}


	private static double f(double x)
	{
		if (x >= 0.0031308)
		{
			return (1.055) * Math.pow(x, 1.0 / GAMMA) - 0.055;
		}
		return 12.92 * x;
	}


	private static double f_inv(double x)
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
