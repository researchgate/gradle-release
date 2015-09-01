/**
 * 
 */
package net.researchgate.release;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author gus
 *
 */
public class SemanticVersionTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public final void parseValidReleaseVersion() {
		SemanticVersion version = new SemanticVersion("1.0.0");
		Assert.assertEquals(1, version.getMajor());
		Assert.assertEquals(0, version.getMinor());
		Assert.assertEquals(0, version.getPatch());
	}
	
	@Test
	public final void parseValidSnapshotVersion() {
		SemanticVersion version = new SemanticVersion("1.0.0-SNAPSHOT");
		Assert.assertEquals(1, version.getMajor());
		Assert.assertEquals(0, version.getMinor());
		Assert.assertEquals(0, version.getPatch());
	}
	
	@Test
	public final void parseInvalidSemanticVersion() {
		expectedException.expect(IllegalArgumentException.class);
		new SemanticVersion("1.0");
	}
	
	@Test
	public final void parseInvalidVersion() {
		expectedException.expect(NumberFormatException.class);
		new SemanticVersion("1.0.0-RC");
	}
	
	@Test
	public final void parseSemanticVersionWithDashesInsteadOfPoints() {
		expectedException.expect(IllegalArgumentException.class);
		new SemanticVersion("1-0-0");
	}
	
	@Test
	public final void twoInstancesAreEqual() {
		SemanticVersion v1 = new SemanticVersion(1, 0, 0);
		SemanticVersion v2 = new SemanticVersion("1.0.0");
		Assert.assertEquals(v1, v2);
	}
	
	@Test
	public final void incrementMinorVersion() {
		SemanticVersion version = new SemanticVersion(1, 0, 0);
		SemanticVersion newVersion = version.newFeature();
		Assert.assertEquals(1, version.getMajor());
		Assert.assertEquals(0, version.getMinor());
		Assert.assertEquals(0, version.getPatch());
		
		Assert.assertEquals(1, newVersion.getMajor());
		Assert.assertEquals(1, newVersion.getMinor());
		Assert.assertEquals(0, newVersion.getPatch());
	}
	
	@Test
	public final void incrementMinorVersionWhenPatchesExist() {
		SemanticVersion version = new SemanticVersion(1, 0, 4);
		SemanticVersion newVersion = version.newFeature();
		Assert.assertEquals(1, version.getMajor());
		Assert.assertEquals(0, version.getMinor());
		Assert.assertEquals(4, version.getPatch());
		
		Assert.assertEquals(1, newVersion.getMajor());
		Assert.assertEquals(1, newVersion.getMinor());
		Assert.assertEquals(0, newVersion.getPatch());
	}
	
	@Test
	public final void incrementPatchVersion() {
		SemanticVersion version = new SemanticVersion(1, 0, 0);
		SemanticVersion newVersion = version.newPatch();
		Assert.assertEquals(1, version.getMajor());
		Assert.assertEquals(0, version.getMinor());
		Assert.assertEquals(0, version.getPatch());
		
		Assert.assertEquals(1, newVersion.getMajor());
		Assert.assertEquals(0, newVersion.getMinor());
		Assert.assertEquals(1, newVersion.getPatch());
	}
	
	@Test
	public final void incrementMajorVersionWhenFeaturesAndPatchesExist() {
		SemanticVersion version = new SemanticVersion(1, 2, 3);
		SemanticVersion newVersion = version.newMajor();
		Assert.assertEquals(1, version.getMajor());
		Assert.assertEquals(2, version.getMinor());
		Assert.assertEquals(3, version.getPatch());
		
		Assert.assertEquals(2, newVersion.getMajor());
		Assert.assertEquals(0, newVersion.getMinor());
		Assert.assertEquals(0, newVersion.getPatch());
	}
	
	@Test
	public final void convertToString() {
		String version = "1.0.0";
		Assert.assertEquals(version, new SemanticVersion(version).toString());
	}
}
