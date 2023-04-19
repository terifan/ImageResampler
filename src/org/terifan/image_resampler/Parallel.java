package org.terifan.image_resampler;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class Parallel
{
	private final int mFrom;
	private final int mTo;
	private final int mStep;
	private final boolean mInclusive;


	private Parallel(int aFrom, int aTo, int aStep, boolean aInclusive)
	{
		if (aStep <= 0)
		{
			throw new IllegalArgumentException("Step must be positive.");
		}

		mFrom = aFrom;
		mTo = aTo;
		mStep = aStep;
		mInclusive = aInclusive;
	}


	public static Parallel range(int aFrom, int aToExclusive)
	{
		return new Parallel(aFrom, aToExclusive, 1, false);
	}


	public static Parallel rangeClosed(int aFrom, int aToInclusive)
	{
		return new Parallel(aFrom, aToInclusive, 1, true);
	}


	public static Parallel range(int aFrom, int aToExclusive, int aStep)
	{
		return new Parallel(aFrom, aToExclusive, aStep, false);
	}


	public static Parallel rangeClosed(int aFrom, int aToInclusive, int aStep)
	{
		return new Parallel(aFrom, aToInclusive, aStep, true);
	}


	/**
	 * Calls the Consumer with a value.
	 * <pre>
	 * Parallel.range(0, 10, 4).forEach(i->System.out.println(i));
	 *   "0"
	 *   "4"
	 *   "8"
	 *
	 * Parallel.rangeClosed(0, 10, 5).forEach(i->System.out.println(i));
	 *   "0"
	 *   "5"
	 *   "10"
	 *
	 * Parallel.range(0, 3).forEach(i->System.out.println(i));
	 *   "0"
	 *   "1"
	 *   "2"
	 *
	 * Parallel.rangeClosed(0, 3).forEach(i->System.out.println(i));
	 *   "0"
	 *   "1"
	 *   "2"
	 *   "3"
	 * </pre>
	 */
	public void forEach(Consumer<Integer> aConsumer)
	{
		int cpu = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

		ExecutorService executor = new ThreadPoolExecutor(cpu, cpu, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

		int to = mInclusive ? mTo + 1 : mTo;
		for (int i = mFrom; i < to; i += mStep)
		{
			int _i = Math.min(i, to);
			executor.submit(() -> aConsumer.accept(_i));
		}

		try
		{
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e)
		{
		}
	}


	/**
	 * Calls the BiConsumer with a starting value and ending value equal to starting value plus step size.
	 * <pre>
	 * Parallel.range(0, 10, 4).forEach((i,j)->System.out.println(i+", "+j));
	 *   "0, 3"
	 *   "4, 7"
	 *   "8, 9"
	 *
	 * Parallel.rangeClosed(0, 10, 4).forEach((i,j)->System.out.println(i+", "+j));
	 *   "0, 4"
	 *   "4, 8"
	 *   "8, 10"
	 * </pre>
	 */
	public void forEach(BiConsumer<Integer, Integer> aConsumer)
	{
		int cpu = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

		ExecutorService executor = new ThreadPoolExecutor(cpu, cpu, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

		for (int i = mFrom; i < mTo; i += mStep)
		{
			int _i = i;
			int _j = Math.min(i + mStep, mTo) - (mInclusive ? 0 : 1);
			executor.submit(() -> aConsumer.accept(_i, _j));
		}

		try
		{
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e)
		{
		}
	}


//	public static void main(String... args)
//	{
//		try
//		{
//			Parallel.range(0, 10, 4).forEach((i) -> System.out.println(i));
//			System.out.println("-----");
//			Parallel.rangeClosed(0, 10, 5).forEach((i) -> System.out.println(i));
//			System.out.println("-----");
//			Parallel.range(0, 3).forEach((i) -> System.out.println(i));
//			System.out.println("-----");
//			Parallel.rangeClosed(0, 3).forEach((i) -> System.out.println(i));
//			System.out.println("-----");
//			Parallel.range(0, 10, 4).forEach((i, j) -> System.out.println(i + ", " + j));
//			System.out.println("-----");
//			Parallel.rangeClosed(0, 10, 4).forEach((i, j) -> System.out.println(i + ", " + j));
//			System.out.println("-----");
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}
}
