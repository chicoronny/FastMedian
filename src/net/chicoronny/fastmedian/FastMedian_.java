package net.chicoronny.fastmedian;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class FastMedian_<T extends RealType<T> & NativeType<T>> implements PlugInFilter {

	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = FastMedian_.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the sample
		ImagePlus image = new ImagePlus(System.getProperty("user.home")+"/ownCloud/exp-images.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

	private ImagePlus image;
	private int numberOfFrames;
	private Img<T> imageA;
	private Img<T> imageB;

	@Override
	public void run(ImageProcessor ip) {
		final GenericDialog gd = new GenericDialog("Fast Median Filter");
		gd.addMessage("Approx. blockwise Median Filter with a 3x3 kernel \n and the requested frames for the 3rd diminsion");
		gd.addNumericField("Number of frames:", 50, 0);
		gd.showDialog();  
        if (gd.wasCanceled()) return;  
		numberOfFrames = (int)gd.getNextNumber();  
		processStack(image, numberOfFrames);
		//ImageJFunctions.show(filtered);
	}

	@Override
	public int setup(String arg, ImagePlus imagePlus) {
		image = imagePlus;
		return DOES_8G | DOES_16 | DOES_32 ;
	}
	
	public void processStack(ImagePlus img, int numberOfFrames){
		IJ.showStatus("FastMedianFilter");
		final ImageStack stack = img.getStack();
		final int stackSize = stack.getSize();
		final Queue<Img<T>> imageList = new ArrayDeque<>();
		
		for (int i = 1; i <= stackSize; i++) {
			Object ip = stack.getPixels(i);
			Img<T> curImage = Utils.wrap(ip, new long[]{stack.getWidth(), stack.getHeight()});
			imageList.add(curImage);
			IJ.showProgress((double)i/stackSize);
			if (i % numberOfFrames == 0){
				process(imageList);
				if (imageA != null) 
					interpolate(imageList);
				imageA = imageB;
				imageList.clear();
			}
		}
		process(imageList);
		if (imageA != null) 
			interpolate(imageList);
		img.updateAndDraw();
		return;
	}	
	
	private void process(final Queue<Img<T>> list) {
		if (list.isEmpty()) return;
		imageB = list.peek();
		long[] dims = new long[imageB.numDimensions()];
		imageB.dimensions(dims);

		final FinalInterval shrinked = Intervals.expand(imageB, -1); // handle borders 
		final IntervalView<T> source = Views.interval(imageB, shrinked);
		final RectangleShape outshape = new RectangleShape(1, false); // 3x3 kernel
		Cursor<T> outcursor = Views.iterable(source).cursor();

		List<RandomAccess<T>> cursorList = new ArrayList<RandomAccess<T>>();

		for (Img<T> currentInterval : list) {
			cursorList.add(currentInterval.randomAccess()); // creating neighborhoods
		}

		for (final Neighborhood<T> localNeighborhood : outshape.neighborhoods(source)) {
			outcursor.fwd();
			final Cursor<T> localCursor = localNeighborhood.cursor();
			final List<Double> values = new ArrayList<Double>();
			while (localCursor.hasNext()) {
				localCursor.fwd();
				for (RandomAccess<T> currentCursor : cursorList) {
					currentCursor.setPosition(localCursor);
					values.add(currentCursor.get().getRealDouble());
				}
			}
			final Double median = QuickSelect.fastmedian(values, values.size()); 
			if (median != null)
				outcursor.get().setReal(median);
		}

		// Borders
		final Cursor<T> top = Views.interval(imageB, Intervals.createMinMax(0, 0, dims[0] - 1, 0)).cursor();
		while (top.hasNext()) {
			final List<Double> values = new ArrayList<Double>();
			top.fwd();
			for (RandomAccess<T> currentCursor : cursorList) {
				currentCursor.setPosition(top);
				values.add(currentCursor.get().getRealDouble());
			}
			final Double median = QuickSelect.fastmedian(values, values.size()); 
			if (median != null)
				top.get().setReal(median);
		}
		final Cursor<T> left = Views.interval(imageB, Intervals.createMinMax(0, 1, 0, dims[1] - 2)).cursor();
		while (left.hasNext()) {
			final List<Double> values = new ArrayList<Double>();
			left.fwd();
			for (RandomAccess<T> currentCursor : cursorList) {
				currentCursor.setPosition(left);
				values.add(currentCursor.get().getRealDouble());
			}
			final Double median = QuickSelect.fastmedian(values, values.size()); 
			if (median != null)
				left.get().setReal(median);
		}
		final Cursor<T> right = Views.interval(imageB, Intervals.createMinMax(dims[0] - 1, 1, dims[0] - 1, dims[1] - 2))
				.cursor();
		while (right.hasNext()) {
			final List<Double> values = new ArrayList<Double>();
			right.fwd();
			for (RandomAccess<T> currentCursor : cursorList) {
				currentCursor.setPosition(right);
				values.add(currentCursor.get().getRealDouble());
			}
			final Double median = QuickSelect.fastmedian(values, values.size()); 
			if (median != null)
				right.get().setReal(median);
		}
		final Cursor<T> bottom = Views.interval(imageB, Intervals.createMinMax(0, dims[1] - 1, dims[0] - 1, dims[1] - 1))
				.cursor();
		while (bottom.hasNext()) {
			final List<Double> values = new ArrayList<Double>();
			bottom.fwd();
			for (RandomAccess<T> currentCursor : cursorList) {
				currentCursor.setPosition(bottom);
				values.add(currentCursor.get().getRealDouble());
			}
			final Double median = QuickSelect.fastmedian(values, values.size()); 
			if (median != null)
				bottom.get().setReal(median);
		}

		return;
	}
	
	private void interpolate(Queue<Img<T>> imageList) {
		double newValue;
		final int nFrames = imageList.size();
		for (int i = 0; i < nFrames; i++) {
			Cursor<T> cursorA = Views.iterable(imageA).cursor();
			Cursor<T> cursorB = Views.iterable(imageB).cursor();
			Img<T> out = imageList.poll();
			Cursor<T> outCursor = Views.iterable(out).cursor();
			while (cursorA.hasNext()) {
				cursorA.fwd();
				cursorB.fwd();
				outCursor.fwd();
				newValue = cursorA.get().getRealDouble()
					+ Math.round((cursorB.get().getRealDouble() - cursorA.get().getRealDouble()) * ((double) i + 1) / nFrames);
				outCursor.get().setReal(newValue);
			}
		}
	}


}
