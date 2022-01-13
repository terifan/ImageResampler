package dev;


public class ColorMath
{
	private final static double GAMMA = 2.4;


	public static int toRGB(Color3d aColor)
	{
		int r = mul8(aColor.r) << 16;
		int g = mul8(aColor.g) << 8;
		int b = mul8(aColor.b);

		return 0xff000000 | r + g + b;
	}


	public static int toSRGB(Color3d aColor)
	{
		int r = mul8(f(aColor.r)) << 16;
		int g = mul8(f(aColor.g)) << 8;
		int b = mul8(f(aColor.b));

		return 0xff000000 | r + g + b;
	}


	public static Color3d fromRGB(int aColor)
	{
		Color3d c = new Color3d();
		c.r = (0xff & (aColor >> 16)) / 255f;
		c.g = (0xff & (aColor >>  8)) / 255f;
		c.b = (0xff & (aColor      )) / 255f;
		return c;
	}


	public static Color3d fromSRGB(int aColor)
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
