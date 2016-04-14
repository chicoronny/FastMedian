package net.chicoronny.fastmedian;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;

public class Utils {
	@SuppressWarnings("unchecked")
	public static <T extends NativeType<T>> Img<T> wrap(Object ip, long[] dims) {
		
		String className = ip.getClass().getName();

		Img<T> theImage = null;
		if (className.contains("[S")) {
			theImage = (Img<T>) ArrayImgs.unsignedShorts((short[]) ip, dims);
		} else if (className.contains("[F")) {
			theImage = (Img<T>) ArrayImgs.floats((float[]) ip, dims);
		} else if (className.contains("[B")) {
			theImage = (Img<T>) ArrayImgs.unsignedBytes((byte[]) ip, dims);
		} else if (className.contains("[I")) {
			theImage = (Img<T>) ArrayImgs.unsignedInts((int[]) ip, dims);
		} else if (className.contains("[D")) {
			theImage = (Img<T>) ArrayImgs.doubles((double[]) ip, dims);
		}
		return theImage;
	}
	
	public static <T extends Comparable<T> & Type<T>> T computeMax(final IterableInterval<T> input) {
		/// create a cursor for the image (the order does not matter)
		final Cursor<T> cursor = input.cursor();

		// initialize min and max with the first image value
		T type = cursor.next();
		T max = type.copy();

		// loop over the rest of the data and determine min and max value
		while (cursor.hasNext()) {
			// we need this type more than once
			type = cursor.next();

			if (type.compareTo(max) > 0)
				max.set(type);
		}
		return max;
	}	
}