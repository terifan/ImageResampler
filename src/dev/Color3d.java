package dev;


public class Color3d
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


	public Color3d add(Color3d c)
	{
		r += c.r;
		g += c.g;
		b += c.b;
		return this;
	}


	public Color3d scale(double s)
	{
		r *= s;
		g *= s;
		b *= s;
		return this;
	}
}
