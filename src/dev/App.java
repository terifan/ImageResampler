package dev;

import dev.Util.ImagePanel;
import static dev.Util.createDiff;
import static dev.Util.resizeDown;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.terifan.image_resampler.FilterFactory;
import org.terifan.image_resampler.ImageResampler;


public class App
{
	public static void main(String... args)
	{
		try
		{
			BufferedImage inImage = ImageIO.read(new File("D:\\Pictures\\Image Compression Suit\\Lenna.png"));
//			BufferedImage inImage = ImageIO.read(new File("D:\\Pictures\\Image Compression Suit\\Cologne Cathedral.png"));
//			BufferedImage inImage = ImageIO.read(new File("D:\\Pictures\\Image Compression Suit\\Swallowtail.png"));
//			BufferedImage inImage = ImageIO.read(new File("D:\\Pictures\\Image Compression Suit\\77 Bombay street.png"));
//			BufferedImage inImage = ImageIO.read(new File("D:\\Pictures\\Image Compression Suit\\Erfurt Cathedral.png"));

			int targetW = 200;
			int targetH = 200;

			BufferedImage image1 = ImageResampler.getScaledImage(inImage, targetW, targetH, !false, FilterFactory.Sinc);

			BufferedImage image2 = SincImageResampler.resampleImage(inImage, targetW, targetH);
//			BufferedImage image2 = resize(inImage, targetW, targetH, false, FilterFactory.Sinc);

			BufferedImage refImage = resizeDown(inImage, targetW, targetH);
//			BufferedImage refImage = ImageIO.read(new File("D:\\Pictures\\Image Compression Suit\\77 Bombay street x200.png"));

			System.out.print("image1 / refImage: ");
			BufferedImage diff1 = createDiff(image1, refImage);
			System.out.print("image2 / refImage: ");
			BufferedImage diff2 = createDiff(image2, refImage);
			System.out.print("image1 /   image2: ");
			BufferedImage diffImage3 = createDiff(image1, image2);
//			System.out.print("    diff1 / diff2: ");
//			BufferedImage diffImage3 = createDiff(diff1, diff2);

			JPanel panel = new JPanel(new GridLayout(2, 3, 5, 5));
			panel.add(new ImagePanel(image1));
			panel.add(new ImagePanel(refImage));
			panel.add(new ImagePanel(diff1));
			panel.add(new ImagePanel(image2));
			panel.add(new ImagePanel(diffImage3));
			panel.add(new ImagePanel(diff2));

			JFrame frame = new JFrame();
			frame.add(panel);
			frame.setSize(740 * 3, 700 * 2);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);

//			ImageIO.write(outImage, "png", new File("d:\\dev\\output.png"));
//			ImageIO.write(diffImage, "png", new File("d:\\dev\\output2.png"));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
