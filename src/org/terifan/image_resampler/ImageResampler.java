package org.terifan.image_resampler;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import org.terifan.image_resampler.FilterFactory.Filter;


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

		Color3d[][] pixels = new Color3d[srcHeight][srcWidth];

		for (int y = 0; y < srcHeight; y++)
		{
			for (int x = 0; x < srcWidth; x++)
			{
				if (aSRGB)
				{
					pixels[y][x] = fromSRGB(aImage.getRGB(x, y));
				}
				else
				{
					pixels[y][x] = fromRGB(aImage.getRGB(x, y));
				}
			}
		}

		double[] kernelH = aFilter.getKernel1D(Math.max(2, srcWidth * 2 / aDstWidth) | 1);
		double[] kernelV = aFilter.getKernel1D(Math.max(2, srcHeight * 2 / aDstHeight) | 1);

		pixels = transpose(resample(pixels, srcWidth, srcHeight, aDstWidth, kernelH));
		pixels = transpose(resample(pixels, srcHeight, aDstWidth, aDstHeight, kernelV));

		BufferedImage outImage = new BufferedImage(aDstWidth, aDstHeight, BufferedImage.TYPE_INT_RGB);

		for (int sy = 0; sy < aDstHeight; sy++)
		{
			for (int sx = 0; sx < aDstWidth; sx++)
			{
				Color3d c = pixels[sy][sx];

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


	private static Color3d[][] resample(Color3d[][] aInput, int aSrcWidth, int aSrcHeight, int aNewWidth, double[] aKernel)
	{
		Color3d[][] output = new Color3d[aSrcHeight][aNewWidth];

		int filterLen = aKernel.length;

		for (int y = 0; y < aSrcHeight; y++)
		{
			for (int x = 0; x < aNewWidth; x++)
			{
				double centerX = (0.5 + x) / aNewWidth * aSrcWidth;
				double filterStartX = centerX - filterLen / 2.0;
				int startIndex = (int)Math.ceil(filterStartX - 0.5);

				double r = 0;
				double g = 0;
				double b = 0;
				double w = 0;

				for (int f = 0; f < filterLen; f++)
				{
					int inputX = startIndex + f;

					double z = (inputX + 0.5 - filterStartX) / (filterLen);
					double window = 0 <= z && z <= 1 ? 1 - Math.abs(z - 0.5) * 2 : 0;
					double k = aKernel[f] * window;

					Color3d c = aInput[y][Math.min(Math.max(inputX, 0), aSrcWidth - 1)];

					r += k * c.r;
					g += k * c.g;
					b += k * c.b;
					w += k;
				}

				output[y][x] = new Color3d(r / w, g / w, b / w);
			}
		}

		return output;
	}


	private static Color3d[][] transpose(Color3d[][] input)
	{
		Color3d[][] output = new Color3d[input[0].length][input.length];

		for (int y = 0; y < input.length; y++)
		{
			for (int x = 0; x < input[0].length; x++)
			{
				output[x][y] = input[y][x];
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


	private static class Color3d
	{
		double r, g, b;


		public Color3d()
		{
		}


		public Color3d(double aR, double aG, double aB)
		{
			this.r = aR;
			this.g = aG;
			this.b = aB;
		}
	}


	private final static double GAMMA = 2.4;


	private static int toRGB(Color3d aColor)
	{
		int r = mul8(aColor.r) << 16;
		int g = mul8(aColor.g) << 8;
		int b = mul8(aColor.b);

		return 0xff000000 | r + g + b;
	}


	private static int toSRGB(Color3d aColor)
	{
		int r = mul8(f(aColor.r)) << 16;
		int g = mul8(f(aColor.g)) << 8;
		int b = mul8(f(aColor.b));

		return 0xff000000 | r + g + b;
	}


	private static Color3d fromRGB(int aColor)
	{
		Color3d c = new Color3d();
		c.r = (0xff & (aColor >> 16)) / 255f;
		c.g = (0xff & (aColor >>  8)) / 255f;
		c.b = (0xff & (aColor      )) / 255f;
		return c;
	}


	private static Color3d fromSRGB(int aColor)
	{
		Color3d c = new Color3d();
		c.r = f_inv((0xff & (aColor >> 16)) / 255.0);
		c.g = f_inv((0xff & (aColor >> 8)) / 255.0);
		c.b = f_inv((0xff & (aColor)) / 255.0);
		return c;
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
