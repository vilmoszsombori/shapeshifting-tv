package tv.shapeshifting.nsl.util;

import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;

public class Timecode implements Comparable<Object> {

	/*
	 * Static helpers to convert between SMPTE timecode and milliseconds
	 */

	private static boolean isValidSMPTE(String smpteTimecode) {
		boolean ret = false;
		if (smpteTimecode != null && !smpteTimecode.isEmpty()) {
			String[] temp = smpteTimecode.toString().split(":");
			if (temp.length == 4)
				return true;
		}
		return ret;
	}

	public static Timecode parse(Object time) throws TimecodeFormatException {
		if (time == null)
			throw new TimecodeFormatException("Null timecode");
		String timeString = time.toString().trim().toLowerCase();
		if (timeString.isEmpty())
			throw new TimecodeFormatException("Empty string");
		if (isValidSMPTE(timeString))
			return parseSMPTE(timeString);
		else if (timeString.endsWith("ms")) {
			try {
				return new Timecode(Long.parseLong(timeString.substring(0,
						timeString.length() - 2)));
			} catch (NumberFormatException e) {
				throw new TimecodeFormatException("Unexpected number format ["
						+ timeString + "]");
			}
		} else if (timeString.endsWith("s")) {
			try {
				return new Timecode(Double.valueOf(
						Double.parseDouble(timeString.substring(0,
								timeString.length() - 1)) * 1000).longValue());
			} catch (NumberFormatException e) {
				throw new TimecodeFormatException("Unexpected number format ["
						+ timeString + "]");
			}
		} else {
			try {
				return new Timecode(Double.valueOf(timeString).longValue());
			} catch (NumberFormatException e) {
				throw new TimecodeFormatException("Unexpected number format ["
						+ timeString + "]");
			}
		}
	}

	public static Timecode parseSMPTE(String smpteTimecode)
			throws TimecodeFormatException {
		if (!isValidSMPTE(smpteTimecode))
			throw new TimecodeFormatException("SMPTE timecode expected ["
					+ smpteTimecode + "]");
		String[] temp = smpteTimecode.toString().split(":");
		long millis = ((Integer.parseInt(temp[0]) * 3600
				+ Integer.parseInt(temp[1]) * 60 + Integer.parseInt(temp[2])) * 25 + Integer
				.parseInt(temp[3])) * 40;
		return new Timecode(millis);
	}

	public static Timecode valueOf(int time) throws TimecodeFormatException {
		return new Timecode(Integer.valueOf(time).longValue());
	}

	public static Timecode valueOf(long time) throws TimecodeFormatException {
		return new Timecode(time);
	}

	private long time;

	public Timecode(long time) throws TimecodeFormatException {
		if (time < 0)
			throw new TimecodeFormatException("Positive long expected [" + time
					+ "]");
		this.time = time;
	}

	public int intValue() {
		return Long.valueOf(time).intValue();
	}

	public long longValue() {
		return Long.valueOf(time).longValue();
	}

	public String toMilliseconds() {
		return time + "ms";
	}

	public String toSeconds() {
		return (Long.valueOf(time).doubleValue() / 1000) + "s";
	}

	public String toSMPTE() {
		String hours = String.valueOf(time / 3600000);
		if (hours.length() == 1)
			hours = "0" + hours;
		String minutes = String.valueOf((time % 3600000) / 60000);
		if (minutes.length() == 1)
			minutes = "0" + minutes;
		String seconds = String.valueOf(((time % 3600000) % 60000) / 1000);
		if (seconds.length() == 1)
			seconds = "0" + seconds;
		String frames = String
				.valueOf((((time % 3600000) % 60000) % 1000) / 40);
		if (frames.length() == 1)
			frames = "0" + frames;
		return hours + ":" + minutes + ":" + seconds + ":" + frames;
	}

	@Override
	public String toString() {
		return String.valueOf(time);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Timecode other = (Timecode) obj;
		if (time != other.time)
			return false;
		return true;
	}

	@Override
	public int compareTo(Object time) {
		return compareTo((Timecode)time);
	}
	
	public int compareTo(Timecode time) {
		return intValue() - time.intValue();
	}

}
