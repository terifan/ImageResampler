package dev;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.terifan.image_resampler.FilterFactory;
import org.terifan.image_resampler.ImageResampler;
import org.terifan.image_resampler.ImageResamplerFast;
import org.terifan.image_resampler.ImageResamplerFast.Quality;


public class App
{
	private static BufferedImage inImage;
	private static BufferedImage refImage;
	private static long refTime;


	public static void main(String... args)
	{
		try
		{
//			JComboBox fileList = new JComboBox(new File("D:\\Pictures\\Image Compression Suit").listFiles(f->f.isFile()));
//			JComboBox fileList = new JComboBox(new File("D:\\Pictures\\Wallpapers").listFiles(f->f.isFile()));
//			JComboBox fileList = new JComboBox(new File("D:\\Pictures\\Wallpapers\\4k").listFiles(f->f.isFile()));
			JComboBox fileList = new JComboBox(new File("C:\\Users\\patrik\\Pictures").listFiles(f->f.isFile()));

			int targetW = 64;
			int targetH = 64;

			JPanel panelImages = new JPanel(new GridLayout(2, 3, 5, 5));

			JCheckBox srgb1 = new JCheckBox("SRGB", true);
			JCheckBox srgb2 = new JCheckBox("SRGB", true);

			JList list1 = new JList(FilterFactory.values());
			JList list2 = new JList(FilterFactory.values());
			list1.setSelectedIndex(0);
			list2.setSelectedIndex(0);

			ListSelectionListener filterListener = new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent aEvent)
				{
					if (aEvent == null || !aEvent.getValueIsAdjusting())
					{
						try
						{
							panelImages.removeAll();
							panelImages.validate();
							long t0 = System.currentTimeMillis();
							FilterFactory.Filter filter1 = FilterFactory.values()[list1.getSelectedIndex()];
							BufferedImage filteredImage1 = ImageResampler.getScaledImage(inImage, targetW, targetH, srgb1.isSelected(), filter1);
							long t1 = System.currentTimeMillis();
							FilterFactory.Filter filter2 = FilterFactory.values()[list2.getSelectedIndex()];
							BufferedImage filteredImage2 = ImageResampler.getScaledImage(inImage, targetW, targetH, srgb2.isSelected(), filter2);
							long t2 = System.currentTimeMillis();
							int jpeg1 = pack(filteredImage1);
							int jpeg2 = pack(filteredImage2);
							int jpeg3 = pack(refImage);
							panelImages.add(new ImagePanel("Filter " + filter1.getName() + " " + (t1-t0) + "ms (JPEG "+jpeg1+" bytes)", filteredImage1));
							panelImages.add(new ImagePanel("Filter " + filter2.getName() + " " + (t2-t1) + "ms (JPEG "+jpeg2+" bytes)", filteredImage2));
							panelImages.add(new ImagePanel("Java Bicubic " + refTime + "ms (JPEG "+jpeg3+" bytes)", refImage));
							panelImages.add(new ImagePanel(filter1.getName() + " DIFF " + filter2.getName(), createDiff(filteredImage1, filteredImage2)));
							panelImages.add(new ImagePanel(filter1.getName() + " XOR " + filter2.getName(), createXor(filteredImage1, filteredImage2)));
							panelImages.add(new ImagePanel(filter1.getName() + " DIFF Java Bicubic", createDiff(filteredImage1, refImage)));
							panelImages.validate();
							panelImages.repaint();
						}
						catch (Exception e)
						{
							e.printStackTrace(System.out);
						}
					}
				}
			};
			ActionListener changeListener = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent aE)
				{
					filterListener.valueChanged(null);
				}
			};
			srgb1.addActionListener(changeListener);
			srgb2.addActionListener(changeListener);

			ActionListener fileListener = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent aE)
				{
					try
					{
						inImage = ImageIO.read((File)fileList.getModel().getSelectedItem());
						long t0 = System.currentTimeMillis();
						refImage = ImageResamplerFast.getScaledImage(inImage, targetW, targetH, Quality.BICUBIC);
						long t1 = System.currentTimeMillis();
						refTime = t1 - t0;
						filterListener.valueChanged(null);
					}
					catch (Exception e)
					{
						e.printStackTrace(System.out);
					}
				}
			};
			fileList.addActionListener(fileListener);
			fileList.setSelectedIndex(0);

			list1.addListSelectionListener(filterListener);
			list2.addListSelectionListener(filterListener);

			fileList.actionPerformed(null);

			JPanel testPanel = new JPanel(new BorderLayout());
			JPanel filtersPanel = new JPanel(new BorderLayout());
			JPanel filter1Panel = new JPanel(new BorderLayout());
			JPanel filter2Panel = new JPanel(new BorderLayout());
			filter1Panel.add(srgb1, BorderLayout.NORTH);
			filter2Panel.add(srgb2, BorderLayout.NORTH);
			filter1Panel.add(new JScrollPane(list1), BorderLayout.CENTER);
			filter2Panel.add(new JScrollPane(list2), BorderLayout.CENTER);
			filtersPanel.add(filter1Panel, BorderLayout.WEST);
			filtersPanel.add(filter2Panel, BorderLayout.EAST);
			testPanel.add(filtersPanel, BorderLayout.WEST);
			testPanel.add(panelImages, BorderLayout.CENTER);

			JPanel filesPanel = new JPanel(new FlowLayout());
			filesPanel.add(new JLabel("File: "));
			filesPanel.add(fileList);

			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(filesPanel, BorderLayout.NORTH);
			mainPanel.add(testPanel, BorderLayout.CENTER);

			JFrame frame = new JFrame("Downscale image to "+targetW+"x"+targetH+" using custom kernals compared to Java's default Bicubic method");
			frame.add(mainPanel);
			frame.setSize(740 * 3, 700 * 2);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


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
				diffImage.setRGB(x, y, c1 ^ c2);
			}
		}

		return diffImage;
	}


	@SuppressWarnings("serial")
	static class ImagePanel extends JPanel
	{
		ImagePanel(String aTitle, BufferedImage aImage)
		{
			setLayout(new BorderLayout());
			add(new JLabel(aTitle), BorderLayout.NORTH);
			add(new JPanel()
			{
				@Override
				protected void paintComponent(Graphics aGraphics)
				{
//					aGraphics.drawImage(aImage, 0, 0, null);
					aGraphics.drawImage(aImage, 0, 0, getHeight(), getHeight(), null);
//					aGraphics.drawImage(aImage, 0, getHeight()-aImage.getHeight(), null);
				}
			}, BorderLayout.CENTER);
		}
	}


	private static int pack(BufferedImage aFilteredImage2) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(aFilteredImage2, "jpeg", baos);
		return baos.size();
	}
}
