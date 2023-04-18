package dev;

import dev.Util.ImagePanel;
import static dev.Util.createDiff;
import static dev.Util.createXor;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFrame;
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
			JComboBox fileList = new JComboBox(new File("D:\\Pictures\\Wallpapers\\4k").listFiles(f->f.isFile()));
//			JComboBox fileList = new JComboBox(new File("D:\\Pictures").listFiles(f->f.isFile()));

			int targetW = 200;
			int targetH = 200;

			JPanel panelImages = new JPanel(new GridLayout(2, 3, 5, 5));

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
						panelImages.removeAll();
						panelImages.validate();
						long t0 = System.currentTimeMillis();
						FilterFactory.Filter filter1 = FilterFactory.values()[list1.getSelectedIndex()];
						BufferedImage filteredImage1 = ImageResampler.getScaledImage(inImage, targetW, targetH, !true, filter1);
						long t1 = System.currentTimeMillis();
						FilterFactory.Filter filter2 = FilterFactory.values()[list2.getSelectedIndex()];
						BufferedImage filteredImage2 = ImageResampler.getScaledImage(inImage, targetW, targetH, !true, filter2);
						long t2 = System.currentTimeMillis();
						panelImages.add(new ImagePanel("Filter " + filter1.getName() + " " + (t1-t0) + "ms", filteredImage1));
						panelImages.add(new ImagePanel("Filter " + filter2.getName() + " " + (t2-t1) + "ms", filteredImage2));
						panelImages.add(new ImagePanel("Java Bicubic " + refTime + "ms", refImage));
						panelImages.add(new ImagePanel(filter1.getName() + " DIFF " + filter2.getName(), createDiff(filteredImage1, filteredImage2)));
						panelImages.add(new ImagePanel(filter1.getName() + " XOR " + filter2.getName(), createXor(filteredImage1, filteredImage2)));
						panelImages.add(new ImagePanel(filter1.getName() + " DIFF Java Bicubic", createDiff(filteredImage1, refImage)));
						panelImages.validate();
						panelImages.repaint();
					}
				}
			};

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

			JPanel mainPanel = new JPanel(new BorderLayout());
			JPanel filtersPanel = new JPanel(new BorderLayout());
			filtersPanel.add(new JScrollPane(list1), BorderLayout.WEST);
			filtersPanel.add(new JScrollPane(list2), BorderLayout.EAST);
			mainPanel.add(filtersPanel, BorderLayout.WEST);
			mainPanel.add(panelImages, BorderLayout.CENTER);

			JPanel filesPanel = new JPanel(new BorderLayout());
			filesPanel.add(fileList, BorderLayout.NORTH);
			filesPanel.add(mainPanel, BorderLayout.CENTER);

			JFrame frame = new JFrame();
			frame.add(filesPanel);
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
}
