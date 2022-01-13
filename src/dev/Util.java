package dev;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;


public class Util
{
	static BufferedImage createDiff(BufferedImage aImage1, BufferedImage aImage2)
	{
		BufferedImage diffImage = new BufferedImage(aImage1.getWidth(), aImage1.getHeight(), BufferedImage.TYPE_INT_RGB);

		int scale = 1;

		int err = 0;
		for (int y = 0; y < aImage1.getHeight(); y++)
		{
			for (int x = 0; x < aImage1.getWidth(); x++)
			{
				int c1 = aImage1.getRGB(x, y);
				int c2 = aImage2.getRGB(x, y);
				int dr = Math.abs((0xff & (c1 >> 16)) - (0xff & (c2 >> 16))) * scale;
				int dg = Math.abs((0xff & (c1 >>  8)) - (0xff & (c2 >>  8))) * scale;
				int db = Math.abs((0xff & (c1 >>  0)) - (0xff & (c2 >>  0))) * scale;
				int r = (128 + dr) << 16;
				int g = (128 + dg) <<  8;
				int b = (128 + db) <<  0;
				diffImage.setRGB(x, y, r + g + b);
				err += dr + dg + db;
			}
		}

		System.out.println(err / (double)aImage1.getHeight() / aImage1.getWidth());

		return diffImage;
	}


	static BufferedImage resizeDown(BufferedImage aSource, int aWidth, int aHeight)
	{
		if (aWidth <= 0 || aHeight <= 0)
		{
			throw new IllegalArgumentException("Target width or height is zero or less: width: " + aWidth + ", height: " + aHeight);
		}

		int currentWidth = aSource.getWidth();
		int currentHeight = aSource.getHeight();
		boolean flush = false;

		do
		{
			if (currentWidth > aWidth)
			{
				currentWidth = Math.max((currentWidth + 1) / 2, aWidth);
			}
			if (currentHeight > aHeight)
			{
				currentHeight = Math.max((currentHeight + 1) / 2, aHeight);
			}

			boolean isFinal = currentWidth == aWidth && currentHeight == aHeight;

			BufferedImage tmp = resizeDownImpl(aSource, currentWidth, currentHeight, isFinal);

			if (flush)
			{
				aSource.flush();
			}

			aSource = tmp;
			flush = true;
		}
		while (currentWidth > aWidth || currentHeight > aHeight);

		return aSource;
	}


	static BufferedImage resizeDownImpl(BufferedImage aSource, int aWidth, int aHeight, boolean aFinal)
	{
		BufferedImage output = new BufferedImage(aWidth, aHeight, aSource.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = output.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, aFinal ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(aSource, 0, 0, aWidth, aHeight, null);
		g.dispose();

		return output;
	}


	static class ImagePanel extends JPanel
	{
		private BufferedImage mImage;


		ImagePanel(BufferedImage aImage)
		{
			mImage = aImage;
		}


		@Override
		protected void paintComponent(Graphics aGraphics)
		{
			aGraphics.drawImage(mImage, 0, 0, getHeight(), getHeight(), null);
//			aGraphics.drawImage(mImage, 0, 0, null);
		}
	}
}
