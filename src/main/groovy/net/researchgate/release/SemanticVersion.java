package net.researchgate.release;

public class SemanticVersion {

	/** The major version of the application that are not backwards compatible. */
	private final int major;
	/** The minor version of the application that represents features. */
	private final int minor;
	/** Patches to the feature version. */
	private final int patch;

	/**
	 * @param major The major version of the application that are not backwards compatible.
	 * @param minor The minor version of the application that represents features.
	 * @param patch Patches to the feature version.
	 */
	public SemanticVersion(final int major, final int minor, final int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	public SemanticVersion(final String version) {
		String releaseVersion = version;
		if (isSnapshot(version)) {
			releaseVersion = releaseVersion.substring(0,
					releaseVersion.indexOf("-SNAPSHOT"));
		}
		String[] semanticVersion = releaseVersion.split("\\.");
		if (semanticVersion.length != 3) {
			throw new IllegalArgumentException(
					"Semantic version should have 3 digits and be in the format x.x.x");
		}
		this.major = Integer.parseInt(semanticVersion[0]);
		this.minor = Integer.parseInt(semanticVersion[1]);
		this.patch = Integer.parseInt(semanticVersion[2]);
	}

	private boolean isSnapshot(final String version) {
		return version.contains("-SNAPSHOT");
	}

	/**
	 * @return the major
	 */
	public final int getMajor() {
		return major;
	}

	/**
	 * @return the minor
	 */
	public final int getMinor() {
		return minor;
	}

	/**
	 * @return the patch
	 */
	public final int getPatch() {
		return patch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + major;
		result = prime * result + minor;
		result = prime * result + patch;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SemanticVersion other = (SemanticVersion) obj;
		if (major != other.major) {
			return false;
		}
		if (minor != other.minor) {
			return false;
		}
		if (patch != other.patch) {
			return false;
		}
		return true;
	}

	/**
	 * @return The version that has the new feature.
	 */
	public SemanticVersion newFeature() {
		return new SemanticVersion(this.major, this.minor + 1, 0);
	}

	/**
	 * @return The version that has the new patch.
	 */
	public SemanticVersion newPatch() {
		return new SemanticVersion(this.major, this.minor, this.patch + 1);
	}

	/**
	 * @return The version that has the new major change.
	 */
	public SemanticVersion newMajor() {
		return new SemanticVersion(this.major + 1, 0, 0);
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}

}
