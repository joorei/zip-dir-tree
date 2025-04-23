package zn.zip;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

import net.lingala.zip4j.model.FileHeader;

/**
 * One instance is not thread save if used in multiple threads. If each instance
 * is used in only one thread this class is thread save.
 */
public class FileHeaderByteConverter {
	/**
	 * Used to generate <code>short</code> values from <code>byte[]</code>.
	 * <p>
	 * We opted to copy the 2 bytes instead of creating a new {@link ByteBuffer}
	 * instance every time using {@link ByteBuffer#wrap(byte[])}.
	 */
	private final ByteBuffer shortBuffer = Objects.requireNonNull(ByteBuffer.allocate(2));

	/**
	 * @param fileHeader Instance to read the general purpose flag from.
	 * @return The general purpose flag as <code>short</code> value.
	 * @throws BufferOverflowException  If the given
	 *                                  {@link FileHeader#getGeneralPurposeFlag()}
	 *                                  returned a byte array with a length bigger
	 *                                  than 2.
	 * @throws BufferUnderflowException If the given
	 *                                  {@link FileHeader#getGeneralPurposeFlag()}
	 *                                  returned a byte array with a length less
	 *                                  than 2.
	 * @throws NullPointerException     If <code>null</code> was given.
	 * @see <a href=
	 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.3.7
	 *      Local file header</a>
	 */
	public short getGeneralPurposeFlag(final FileHeader fileHeader) {
		return this.getShort(Objects.requireNonNull(fileHeader.getGeneralPurposeFlag()));
	}

	/**
	 * @param bytes The bytes to convert to a <code>short</code> value.
	 * @return The converted bytes.
	 * @throws BufferOverflowException  If the length of the given bytes is more
	 *                                  than 2.
	 * @throws BufferUnderflowException If the length of the given bytes is less
	 *                                  than 2.
	 * @throws NullPointerException     If <code>null</code> was given.
	 */
	private short getShort(final byte[] bytes) {
		if (bytes.length != 2) {
			throw new IllegalArgumentException();
		}
		this.shortBuffer.clear();
		this.shortBuffer.mark();
		this.shortBuffer.put(bytes);
		this.shortBuffer.reset();
		if (this.shortBuffer.remaining() != 2) {
			throw new IllegalArgumentException();
		}
		return this.shortBuffer.getShort();
	}
}
