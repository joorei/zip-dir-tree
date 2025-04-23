package zn.zip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

import org.codeturnery.bytes.BytesUtil;
import org.eclipse.jdt.annotation.Checks;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.util.Zip4jUtil;

public class ByteContentReader {
	/**
	 * 
	 * @param fileHeader
	 * @param zipFile
	 * @param byteBuffer
	 * @throws ZipException
	 * @throws IOException
	 * @throws InsufficientBufferException
	 * @throws UnexpectedEndOfInputStreamException
	 * @throws NegativeSizeException
	 * @throws SizeTooLargeException
	 */
	public static void readInto(final FileHeader fileHeader, final ZipFile zipFile, final ByteBuffer byteBuffer)
			throws ZipException, IOException {
		int readLength = readInto(fileHeader, zipFile, byteBuffer.array());
		byteBuffer.position(0);
		byteBuffer.limit(readLength);
	}

	public static FileHeader getFileHeader(final long compressedSize, final long uncompressedSize, final long crc, final long offsetLocalHeader, final int diskNumberStart) {
		final FileHeader syntheticFileHeader = new FileHeader();
		syntheticFileHeader.setCrc(crc);
		syntheticFileHeader.setDiskNumberStart(diskNumberStart);
		syntheticFileHeader.setOffsetLocalHeader(offsetLocalHeader);
		syntheticFileHeader.setCompressedSize(compressedSize);
		syntheticFileHeader.setUncompressedSize(uncompressedSize);
		return syntheticFileHeader;
	}

	/**
	 * Reads the content of the given {@link FileHeader} into the given buffer.
	 * <p>
	 * If and only if assertions are enabled additional checks are performed
	 * checking if the <code>inputStream</code> provides more or less bytes than the
	 * value read from {@link FileHeader#getUncompressedSize()} in which case the
	 * assertions will fail.
	 * 
	 * @param fileHeader
	 * @param buffer     Must be at least the size of
	 *                   {@link FileHeader#getUncompressedSize()}.
	 * @return
	 * @throws IOException
	 * @throws ZipException
	 * @throws UnexpectedEndOfInputStreamException
	 * @throws InsufficientBufferException
	 * @throws SizeTooLargeException
	 * @throws NegativeSizeException
	 * @throws IOException                         Thrown if the read access to the
	 *                                             <code>inputStream</code> fails.
	 * @throws InsufficientBufferException         Thrown if the buffer is smaller
	 *                                             than the
	 *                                             <code>expectedSize</code>.
	 * @throws UnexpectedEndOfInputStreamException Thrown less bytes were read from
	 *                                             the <code>inputStream</code> into
	 *                                             the <code>buffer</code> than the
	 *                                             expectedSize
	 *                                             <code>byteLength</code>.
	 * @throws NullPointerException                If one of the given parameters is
	 *                                             <code>null</code>.
	 */
	public static int readInto(final FileHeader fileHeader, final ZipFile zipFile, final byte[] buffer)
			throws IOException, ZipException {
		try (final ZipInputStream inputStream = zipFile.getInputStream(fileHeader);) {
			final int expectedSize = uncompressedSizeAsInt(fileHeader);
			int overallLengthRead = Zip4jUtil.readFully(inputStream, buffer, 0, expectedSize);
			assert overallLengthRead == expectedSize : "read more data from input stream than expected";
			assert inputStream.read(new byte[1]) == -1 : "missing bytes?";
			return expectedSize;
		}
	}

	/**
	 * Reads the file content of the given file header and updates the given
	 * {@link MessageDigest} using it.
	 * 
	 * @param fileHeader
	 * @param zipFile
	 * @param buffer        Temporary storage to copy bytes from the file content to
	 *                      the {@link MessageDigest}. May be smaller than the bytes
	 *                      to read, bigger sizes may improve performance. Not
	 *                      suited to read the actual content after invocation of
	 *                      this function.
	 * @param messageDigest
	 * @return
	 * @throws ZipException
	 * @throws IOException
	 * @throws InsufficientBufferException
	 * @throws UnexpectedEndOfInputStreamException
	 * @throws NegativeSizeException
	 * @throws SizeTooLargeException
	 * @throws IllegalStateException
	 */
	public static int readInto(final FileHeader fileHeader, final ZipFile zipFile, final byte[] buffer,
			final MessageDigest messageDigest) throws ZipException, IOException {
		try (final ZipInputStream inputStream = zipFile.getInputStream(fileHeader);) {
			final int expectedSize = uncompressedSizeAsInt(fileHeader);
			final int readCount = BytesUtil.readInto(inputStream, buffer, 0, expectedSize, messageDigest);
			if (readCount != expectedSize) {
				throw new IllegalStateException("Expected size (" + expectedSize + ") not equal to read size (" + readCount + ").");
			}
			return expectedSize;
		}
	}

	/**
	 * Reads the uncompressed size as <code>long</code> value and returns it as an
	 * <code>int</code>.
	 * 
	 * @param fileHeader The object to read the uncompressed size from.
	 * @return The casted value.
	 * @throws ArithmeticException Thrown if
	 *                             {@link FileHeader#getUncompressedSize()} returned
	 *                             a negative value orif the uncompressed size is
	 *                             bigger than {@link Integer#MAX_VALUE}.
	 */
	private static int uncompressedSizeAsInt(final FileHeader fileHeader) {
		return asUnsignedInt(fileHeader.getUncompressedSize());

	}

	public static int asUnsignedInt(final long value) throws ArithmeticException {
		if (value < 0) {
			throw new ArithmeticException("size must not be negative: " + value);
		}
		return Math.toIntExact(value);
	}

}
