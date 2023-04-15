package org.terifan.image_resampler;


/**
 * Filters that accept parameters.
 * 
 * e.g. ImageResampler.getScaledImageAspect(image, 500, 300, true, new CustomFilterFactory.Cubic2(0, 0.75));
 */
public class CustomFilterFactory
{
	public final static class BiCubic extends FilterFactory.Filter
	{
		private double A;


		/**
		 * @param aA filter parameter, e.g. -0.5
		 */
		public BiCubic(double aA)
		{
			super("BiCubic", 2.0);
			A = aA;
		}


		@Override
		public double filter(double x)
		{
			if (x == 0)
			{
				return 1.0;
			}
			if (x < 0.0)
			{
				x = -x;
			}
			double vv = x * x;
			if (x < 1.0)
			{
				return (A + 2) * vv * x - (A + 3) * vv + 1;
			}
			if (x < 2.0)
			{
				return A * vv * x - 5 * A * vv + 8 * A * x - 4 * A;
			}
			return 0.0;
		}
	};


	// https://www.intel.com/content/www/us/en/develop/documentation/ipp-dev-reference/top/volume-2-image-processing/ipp-ref-interpolation-in-image-geometry-transform/interpolation-with-two-parameter-cubic-filters.html
	public final static class Cubic2 extends FilterFactory.Filter
	{
		private final double B;
		private final double C;


		/**
		 * B      C     Name                Usage
		 * ---------------------------------------------------------------------------------
		 * 0.00    Any 	Cardinal splines
		 * 0.00   0.50 	Catmull-Rom spline  Bicubic filter in GIMP
		 * 0.00   0.75 	Unnamed             Bicubic filter in Adobe Photoshop[5]
		 * 0.33   0.33 	Mitchell–Netravali  Mitchell filter in ImageMagick[4]
		 * 1.00   0.00 	B-spline            Bicubic filter in Paint.net
		 *
		 * @param aB filter parameter, e.g. 0
		 * @param aC filter parameter, e.g. 0.75
		 */
		public Cubic2(double aB, double aC)
		{
			super("BiCubic", 2.0);
			B = aB;
			C = aC;
		}


		@Override
		public double filter(double x)
		{
			x = Math.abs(x);
			if (x < 1)
			{
				double a = (12 - 9 * B - 6 * C) * x * x * x;
				double b = (-18 + 12 * B + 6 * C) * x * x;
				double c = 6 - 2 * B;
				return a + b + c;
			}
			if (x < 2)
			{
				double a = (-B - 6 * C) * x * x * x;
				double b = (6 * B + 30 * C) * x * x;
				double c = -12 * B - 48 * C * x;
				double d = 8 * B + 24 * C;
				return a + b + c + d;
			}
			return 0;
		}
	};
}
