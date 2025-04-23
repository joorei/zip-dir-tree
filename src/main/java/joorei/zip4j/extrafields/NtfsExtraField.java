package joorei.zip4j.extrafields;

import java.nio.BufferUnderflowException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import net.lingala.zip4j.util.RawIO;

/**
 * @see <a href=
 *      "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.5.5
 *      -NTFS Extra Field (0x000a)</a>
 */
public class NtfsExtraField {
	/**
	 * @see <a href=
	 *      "https://articles.forensicfocus.com/2013/04/06/interpretation-of-ntfs-timestamps/"
	 *      >Interpretation of NTFS Timestamps</a>
	 * @see <a href=
	 *      "https://stackoverflow.com/questions/5398557/java-library-for-dealing-with-win32-filetime"
	 *      >Java library for dealing with win32 FILETIME?</a>
	 */
	private static final Instant NTFS_EPOCH = Instant.parse("1601-01-01T00:00:00Z");

	private static final RawIO RAW_IO = new RawIO();
	/** Tag for this "extra" block type */
	public static final short FIELD_TAG = 0x000a;
	/** The data passed in the constructor */
	private final byte[] rawData;

	/**
	 * All fields are expected to be stored in Intel low-byte/high-byte order.
	 * 
	 * @param data The byte array containing the data of the NTFS field. The two
	 *             bytes for the tag (0x000a) and the two bytes for the tSize must
	 *             not be part of this byte array. Instead the array must contain
	 *             the data that follows as specified by <a href=
	 *             "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">4.5.5
	 *             -NTFS Extra Field (0x000a)</a> and must have the length of tSize
	 *             accordingly. The arrays size is expected to be at least 32 with
	 *             all fields stored as Intel low-byte/high-byte order.
	 * @throws BufferUnderflowException At least 32 bytes expected
	 * @throws IllegalArgumentException
	 */
	public NtfsExtraField(final byte[] data) throws BufferUnderflowException {
		this.rawData = Objects.requireNonNull(data);

		/** Reserved for future use */
		assert RAW_IO.readIntLittleEndian(data, 0) == 0;

		/** Tag for attribute #1 */
		assert RAW_IO.readShortLittleEndian(data, 4) == 0x0001;

		/** Size of attribute #1, in bytes */
		assert RAW_IO.readShortLittleEndian(data, 6) == 24
;
		// additional attributes are ignored (not yet implemented)
	}

	/** File creation time */
	public Instant getCreationTime() {
		return toInstant(24);
	}

	/** File last modification time */
	public Instant getModificationTime() {
		return toInstant(8);
	}

	/** File last access time */
	public Instant getAccessTime() {
		return toInstant(16);
	}
	
	private Instant toInstant(final int offset) {
		return toInstant(RAW_IO.readLongLittleEndian(this.rawData, offset));
	}

	private static Instant toInstant(final long fileTime) {
		final long microsecondsAfterZero = fileTime / 10;
		final long additionalNanoseconds = fileTime % 10 * 100;
		final Instant instant = NTFS_EPOCH
				.plus(microsecondsAfterZero, ChronoUnit.MICROS)
				.plus(additionalNanoseconds, ChronoUnit.NANOS);
		// TODO: Zip4j has a method that sounds like it supports NTFSs strange format, but the returned dates are not as expected, even for unix timestamps
//		assert instant.getEpochSecond() * 1000 == Zip4jUtil.dosToJavaTme(fileTime) : Instant.ofEpochMilli(Zip4jUtil.dosToJavaTme(fileTime));
		return instant;
	}

}
