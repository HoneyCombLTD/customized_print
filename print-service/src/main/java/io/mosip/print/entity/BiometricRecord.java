/**
 * 
 */
package io.mosip.print.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * BIR class with Builder to create data
 * 
 * @author Ramadurai Pandian
 *
 */

@Data
public class BiometricRecord implements Serializable {

	protected VersionType version;
	protected VersionType cbeffversion;
	protected BIRInfo birInfo;
	/**
	 * This can be of any modality, each subtype is an element in this list.
	 * it has type and subtype info in it
	 */
	protected List<BIR> segments;
	
	public BiometricRecord() {
		this.segments = new ArrayList<>();
	}
	
	public BiometricRecord(VersionType version, VersionType cbeffversion, BIRInfo birInfo) {
		this.version = version;
		this.cbeffversion = cbeffversion;
		this.birInfo = birInfo;
		this.segments = new ArrayList<BIR>();
	}	

}
