package joorei.zip4j.extrafields;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.CRC32;

import net.lingala.zip4j.util.RawIO;

/**
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.6.9
 *      -Info-ZIP Unicode Path Extra Field (0x7075)</a>
 */
public class UPathExtraField {

	private static final RawIO RAW_IO = new RawIO();
	public static final short FIELD_TAG = 0x7075;
	private final byte[] rawData;

	/**
	 * @param fieldBytes
	 * @throws BufferUnderflowException At least 5 bytes expected
	 */
	public UPathExtraField(final byte[] fieldBytes) throws BufferUnderflowException {
		this.rawData = Objects.requireNonNull(fieldBytes);

		assert fieldBytes.length >= 5 : "Unexpected end of UPathExtraField buffer";

		/** version */
		assert fieldBytes[0] >= 1 : "A version less 1 is invalid";
		// From https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
		// Changes MAY NOT be backward compatible so this extra field SHOULD
		// NOT be used if the version is not recognized.
		assert fieldBytes[0] == 1 : "version number not recognized";
	}

	/**
	 * CRC32 checksum of the path stored in the ZIP entry header.
	 * <strong>Not</strong> the checksum of {@link #getUnicodeName()}.
	 * <p>
	 * Can be directly compared to the result of {@link CRC32#getValue()}.
	 *
	 * @return The checksum of the path that was stored in the ZIP entry header when
	 *         the value returned by {@link #getUnicodeName()} was saved.
	 */
	public int getNameCrc32() {
		// "all fields stored in Intel low-byte/high-byte order"
		return RAW_IO.readIntLittleEndian(this.rawData, 1);
	}

	/**
	 * From https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
	 * <p>
	 * <blockquote>The NameCRC32 is the standard zip CRC32 checksum of the File Name
	 * field in the header. This is used to verify that the header File Name field
	 * has not changed since the Unicode Path extra field was created. This can
	 * happen if a utility renames the File Name but does not update the UTF-8 path
	 * extra field. If the CRC check fails, this UTF-8 Path Extra Field SHOULD be
	 * ignored and the File Name field in the header SHOULD be used
	 * instead.</blockquote>
	 * 
	 * @return The unicode representation of the original file or folder path the
	 *         ZIP entry this extra field belongs to was created from.
	 */
	public String getUnicodeName() {
		final int offset = 5;
		return new String(this.rawData, offset, this.rawData.length - offset, StandardCharsets.UTF_8);
	}

}
