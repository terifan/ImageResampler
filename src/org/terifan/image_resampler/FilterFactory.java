package org.terifan.image_resampler;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;


/**
 * e.g. ImageResampler.getScaledImageAspect(image, 500, 300, true, FilterFactory.Lanczos3)
 */
public class FilterFactory
{
	public final static int FIXED_POINT_SCALE = 65536;
	public final static int HALF_FIXED_POINT_SCALE = 32768;


	public static FilterFactory.Filter[] values()
	{
		return new FilterFactory.Filter[]
		{
			Blackman,
			Bohman,
			Box,
			Catrom,
			Cubic,
			Gaussian,
			Hamming,
			Hanning,
			Hermite,
			Jinc,
			Kasier,
			Lanczos2,
			Lanczos3,
			Mitchell,
			Quadratic,
			Sinc,
			Triangle,
			Welch
		};
	}


	public static abstract class Filter
	{
		private String mName;
		private double mRadius;


		public Filter(String aName, double aRadius)
		{
			mName = aName;
			mRadius = aRadius;
		}


		public abstract double filter(double x);


		public String getName()
		{
			return mName;
		}


		public double getRadius()
		{
			return mRadius;
		}


		@Override
		public String toString()
		{
			return mName;
		}


		public int[][] getKernel2DInt(int aDiameter)
		{
			double step = mRadius / (aDiameter / 2.0);
			double step2 = step * step;
			double c = (aDiameter - 1) / 2.0;

			double[][] kernel = new double[aDiameter][aDiameter];
			double weight = 0;

			for (int y = 0; y < aDiameter; y++)
			{
				for (int x = 0; x < aDiameter; x++)
				{
					double d = sqrt(((x - c) * (x - c) + (y - c) * (y - c)) * step2);
					double v = filter(d);
					kernel[y][x] = v;
					weight += v;
				}
			}

			double scale = weight == 0 ? 1 : 1.0 / weight;
			int[][] tmp = new int[aDiameter][aDiameter];

			// ensure the weight of the kernel is FIXED_POINT_SCALE

			double bias = 0.5;
			step = 0.25;

			for (int rescale = 10, order = 0; --rescale >= 0;)
			{
				int sum = 0;

				for (int y = 0; y < aDiameter; y++)
				{
					for (int x = 0; x < aDiameter; x++)
					{
						sum += tmp[y][x] = (int)(FIXED_POINT_SCALE * scale * kernel[y][x] + bias);
					}
				}

				if (sum == FIXED_POINT_SCALE)
				{
					break;
				}
				if (rescale == 0 && (aDiameter & 1) == 1)
				{
					tmp[aDiameter / 2][aDiameter / 2] = FIXED_POINT_SCALE + tmp[aDiameter / 2][aDiameter / 2] - sum;
					break;
				}

				int o;
				if (sum < FIXED_POINT_SCALE)
				{
					o = 1;
					bias += step;
				}
				else
				{
					o = 2;
					bias -= step;
				}

				if (order != o)
				{
					step /= 2;
					order = o;
				}
			}

			return tmp;
		}


		public double[] getKernel1D(int aDiameter)
		{
			double step = 1.0 / (aDiameter / 2.0);
			double c = (aDiameter - 1) / 2.0;

			double[] kernel = new double[aDiameter];

			for (int x = 0; x < aDiameter; x++)
			{
				double d = (x - c) * step;
				double v = filter(d);
				kernel[x] = v;
			}

			return kernel;
		}
	}

	public final static Filter Box = new Filter("Box", 0.5)
	{
		@Override
		public double filter(double x)
		{
			if (x < -0.5)
			{
				return 0;
			}
			if (x <= 0.5)
			{
				return 1;
			}
			return 0;
		}
	};

	public final static Filter Triangle = new Filter("Triangle", 1.0)
	{
		@Override
		public double filter(double x)
		{
			if (x < -1)
			{
				return 0;
			}
			if (x < 0)
			{
				return 1 + x;
			}
			if (x < 1)
			{
				return 1 - x;
			}
			return 0;
		}
	};

	public final static Filter Quadratic = new Filter("Quadratic", 1.5)
	{
		@Override
		public double filter(double x)
		{
			if (x < -1.5)
			{
				return 0;
			}
			if (x < -0.5)
			{
				double t = x + 1.5;
				return 0.5 * t * t;
			}
			if (x < 0.5)
			{
				return 0.75 - x * x;
			}
			if (x < 1.5)
			{
				double t = x - 1.5;
				return 0.5 * t * t;
			}
			return 0;
		}
	};


	public final static Filter Cubic = new Filter("Cubic", 2.0)
	{
		@Override
		public double filter(double x)
		{
			if (x < -2.0)
			{
				return 0;
			}
			if (x < -1.0)
			{
				double t = 2 + x;
				return t * t * t / 6;
			}
			if (x < 0.0)
			{
				return (4 + x * x * (-6 + x * -3)) / 6;
			}
			if (x < 1.0)
			{
				return (4 + x * x * (-6 + x * 3)) / 6;
			}
			if (x < 2.0)
			{
				double t = 2 - x;
				return t * t * t / 6;
			}
			return 0;
		}
	};

	public final static Filter Catrom = new Filter("Catrom", 2.0)
	{
		@Override
		public double filter(double x)
		{
			if (x < -2)
			{
				return 0.0;
			}
			if (x < -1)
			{
				return 0.5 * (4.0 + x * (8.0 + x * (5.0 + x)));
			}
			if (x < 0)
			{
				return 0.5 * (2.0 + x * x * (-5.0 + x * -3.0));
			}
			if (x < 1)
			{
				return 0.5 * (2.0 + x * x * (-5.0 + x * 3.0));
			}
			if (x < 2)
			{
				return 0.5 * (4.0 + x * (-8.0 + x * (5.0 - x)));
			}
			return 0.0;
		}
	};

	public final static Filter Gaussian = new Filter("Gaussian", 1.25)
	{
		@Override
		public double filter(double x)
		{
			return sqrt(2.0 / PI) * exp(-2.0 * x * x);
		}
	};

	public final static Filter Sinc = new Filter("Sinc", 5)
	{
		@Override
		public double filter(double x)
		{
			if (x == 0)
			{
				return 1;
			}
			double t = PI * x;
			return sin(t) / t;
		}
	};

	public final static Filter Welch = new Filter("Welch", 1.0)
	{
		@Override
		public double filter(double x)
		{
			if (x < -1.0)
			{
				return 0;
			}
			if (x < 1.0)
			{
				return 1.0 - x * x;
			}
			return 0.0;
		}
	};

	public final static Filter Mitchell = new Filter("Mitchell", 2.0)
	{
		private final static double B = 1.0 / 3.0;
		private final static double C = 1.0 / 3.0;


		@Override
		public double filter(double x)
		{
			if (x < 0.0f)
			{
				x = -x;
			}
			double tt = x * x;
			if (x < 1.0)
			{
				x = (((12.0 - 9.0 * B - 6.0 * C) * (x * tt)) + ((-18.0 + 12.0 * B + 6.0 * C) * tt) + (6.0 - 2 * B));
				return x / 6.0;
			}
			else if (x < 2.0)
			{
				x = (((-1.0 * B - 6.0 * C) * (x * tt)) + ((6.0 * B + 30.0 * C) * tt) + ((-12.0 * B - 48.0 * C) * x) + (8.0 * B + 24 * C));
				return x / 6.0;
			}
			else
			{
				return 0.0;
			}
		}
	};

	public final static Filter Lanczos2 = new Filter("Lanczos2", 2.0)
	{
		private static double sindiv(double x)
		{
			return sin(x) / x;
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
			if (x < 2.0)
			{
				x *= PI;
				return sindiv(x) * sindiv(x / 2.0);
			}
			return 0.0;
		}
	};

	public final static Filter Lanczos3 = new Filter("Lanczos3", 3.0)
	{
		private static double sindiv(double x)
		{
			return sin(x) / x;
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
			if (x < 3.0)
			{
				x *= PI;
				return sindiv(x) * sindiv(x / 3.0);
			}
			return 0.0;
		}
	};

	public final static Filter Hermite = new Filter("Hermite", 1.0)
	{
		@Override
		public double filter(double x)
		{
			if (x < 0.0)
			{
				x = -x;
			}
			if (x < 1.0)
			{
				return (2.0 * x - 3.0) * x * x + 1.0;
			}
			return 0.0;
		}
	};

	public final static Filter Jinc = new Filter("Jinc", 3.0)
	{
		@Override
		public double filter(double x)
		{
			if (x == 0.0)
			{
				return 0.5 * PI;
			}
			return BesselOrderOne(PI * x) / x;
		}


		private double BesselOrderOne(double x)
		{
			if (x == 0.0)
			{
				return 0.0;
			}
			double p = x;
			if (x < 0.0)
			{
				x = -x;
			}
			if (x < 8.0)
			{
				return p * J1(x);
			}
			double q = sqrt((2.0 / (PI * x))) * (P1(x) * (1.0 / sqrt(2.0) * (sin(x) - cos(x))) - 8.0 / x * Q1(x) * (-1.0 / sqrt(2.0) * (sin(x) + cos(x))));
			if (p < 0.0)
			{
				q = -q;
			}
			return q;
		}


		private double J1(double x)
		{
			double Pone[] =
			{
				0.581199354001606143928050809e+21,
				-0.6672106568924916298020941484e+20,
				0.2316433580634002297931815435e+19,
				-0.3588817569910106050743641413e+17,
				0.2908795263834775409737601689e+15,
				-0.1322983480332126453125473247e+13,
				0.3413234182301700539091292655e+10,
				-0.4695753530642995859767162166e+7,
				0.270112271089232341485679099e+4
			};
			double Qone[] =
			{
				0.11623987080032122878585294e+22,
				0.1185770712190320999837113348e+20,
				0.6092061398917521746105196863e+17,
				0.2081661221307607351240184229e+15,
				0.5243710262167649715406728642e+12,
				0.1013863514358673989967045588e+10,
				0.1501793594998585505921097578e+7,
				0.1606931573481487801970916749e+4,
				0.1e+1
			};

			double p = Pone[8];
			double q = Qone[8];
			for (int i = 7; i >= 0; i--)
			{
				p = p * x * x + Pone[i];
				q = q * x * x + Qone[i];
			}
			return p / q;
		}


		private double P1(double x)
		{
			double Pone[] =
			{
				0.352246649133679798341724373e+5,
				0.62758845247161281269005675e+5,
				0.313539631109159574238669888e+5,
				0.49854832060594338434500455e+4,
				0.2111529182853962382105718e+3,
				0.12571716929145341558495e+1
			};
			double Qone[] =
			{
				0.352246649133679798068390431e+5,
				0.626943469593560511888833731e+5,
				0.312404063819041039923015703e+5,
				0.4930396490181088979386097e+4,
				0.2030775189134759322293574e+3,
				0.1e+1
			};

			double p = Pone[5];
			double q = Qone[5];
			for (int i = 4; i >= 0; i--)
			{
				p = p * (8.0 / x) * (8.0 / x) + Pone[i];
				q = q * (8.0 / x) * (8.0 / x) + Qone[i];
			}
			return (p / q);
		}


		private double Q1(double x)
		{
			double Pone[] =
			{
				0.3511751914303552822533318e+3,
				0.7210391804904475039280863e+3,
				0.4259873011654442389886993e+3,
				0.831898957673850827325226e+2,
				0.45681716295512267064405e+1,
				0.3532840052740123642735e-1
			};
			double Qone[] =
			{
				0.74917374171809127714519505e+4,
				0.154141773392650970499848051e+5,
				0.91522317015169922705904727e+4,
				0.18111867005523513506724158e+4,
				0.1038187585462133728776636e+3,
				0.1e+1
			};

			double p = Pone[5];
			double q = Qone[5];
			for (int i = 4; i >= 0; i--)
			{
				p = p * (8.0 / x) * (8.0 / x) + Pone[i];
				q = q * (8.0 / x) * (8.0 / x) + Qone[i];
			}
			return (p / q);
		}
	};

	public final static Filter Bohman = new Filter("Bohman", 1.0)
	{
		@Override
		public double filter(double x)
		{
			if (x < 0)
			{
				x = -x;
			}
			if (x <= 1.0)
			{
				return (1 - x) * cos(PI * x) + sin(PI * x) / PI;
			}
			return 0;
		}
	};

	public final static Filter Hanning = new Filter("Hanning", 1.0)
	{
		@Override
		public double filter(double x)
		{
			return 0.5 + 0.5 * cos(PI * x);
		}
	};

	public final static Filter Hamming = new Filter("Hamming", 1.0)
	{
		@Override
		public double filter(double x)
		{
			return 0.54 + 0.46 * cos(PI * x);
		}
	};

	public final static Filter Blackman = new Filter("Blackman", 1.0)
	{
		@Override
		public double filter(double x)
		{
			return 0.42 + 0.50 * cos(PI * x) + 0.08 * cos(2.0 * PI * x);
		}
	};

	public final static Filter Kasier = new Filter("Kasier", 1.0)
	{
		@Override
		public double filter(double x)
		{
			return kaiser(x, 6.5, 0.0);
		}


		private double kaiser(double x, double a, double b)
		{
			double i0a = 1.0 / bessel_i0(a);

			return bessel_i0(a * sqrt(1.0 - x * x)) * i0a;
		}


		private double bessel_i0(double x)
		{
			double sum = 1.0;
			double y = x * x / 4.0;
			double t = y;
			for (int i = 2; t > 1e-7; i++)
			{
				sum += t;
				t *= y / (i * i);
			}
			return sum;
		}
	};


	public static void main(String... args)
	{
		try
		{
			for (Filter factory : values())
			{
				System.out.println();
				System.out.println("== " + factory + " " + "=".repeat(80));
				System.out.println();

				{
					double[] filter = factory.getKernel1D(9);

					double w = 0;
					for (int x = 0; x < filter.length; x++)
					{
						w += filter[x];
						System.out.printf("%.5f ", filter[x]);
					}
					System.out.println();
					System.out.println("weight=" + w);
				}

				System.out.println();

				{
					int[][] filter = factory.getKernel2DInt(9);

					for (int y = 0; y < filter.length; y++)
					{
						for (int x = 0; x < filter.length; x++)
						{
							System.out.printf("%6d ", filter[y][x]);
						}
						System.out.println();
					}
				}

			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
