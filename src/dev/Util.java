package dev;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class Util
{
	static BufferedImage createDiff(BufferedImage aImage1, BufferedImage aImage2)
	{
		BufferedImage diffImage = new BufferedImage(aImage1.getWidth(), aImage1.getHeight(), BufferedImage.TYPE_INT_RGB);

		int scale = 1;

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
			}
		}

		return diffImage;
	}


	static BufferedImage createXor(BufferedImage aImage1, BufferedImage aImage2)
	{
		BufferedImage diffImage = new BufferedImage(aImage1.getWidth(), aImage1.getHeight(), BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < aImage1.getHeight(); y++)
		{
			for (int x = 0; x < aImage1.getWidth(); x++)
			{
				int c1 = aImage1.getRGB(x, y);
				int c2 = aImage2.getRGB(x, y);
				int dr = Math.abs((0xff & (c1 >> 16)) ^ (0xff & (c2 >> 16)));
				int dg = Math.abs((0xff & (c1 >>  8)) ^ (0xff & (c2 >>  8)));
				int db = Math.abs((0xff & (c1 >>  0)) ^ (0xff & (c2 >>  0)));
				int r = dr << 16;
				int g = dg <<  8;
				int b = db <<  0;
				diffImage.setRGB(x, y, r + g + b);
			}
		}

		return diffImage;
	}


	static class ImagePanel extends JPanel
	{
		private BufferedImage mImage;
		private String mTitle;


		ImagePanel(String aTitle, BufferedImage aImage)
		{
			mImage = aImage;
			mTitle = aTitle;
			setLayout(new BorderLayout());
			add(new JLabel(mTitle), BorderLayout.NORTH);
			add(new JPanel()
			{
				@Override
				protected void paintComponent(Graphics aGraphics)
				{
					aGraphics.drawImage(mImage, 0, 0, getHeight(), getHeight(), null);
				}
			}, BorderLayout.CENTER);
		}
	}
}
