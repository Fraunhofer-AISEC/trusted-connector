package de.fhg.ids.comm.ws.protocol;

/**
 * Definition of FSM states for IDS protocol.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * @author Georg Raess (georg.raess@aisec.fraunhofer.de)
 *
 */
public final class ProtocolState {
	private String id;
	private String description;
	public final int ord;
	private static int upperBound = 0;
	
    /* -------------- States start here ------------ */
	public static final ProtocolState IDSCP_START = 			new ProtocolState("IDSCP:START", 		"Idscp: start of protocol.");
    public static final ProtocolState IDSCP_ERROR = 			new ProtocolState("IDSCP:ERROR", 		"Idscp: error in protocol.");
    public static final ProtocolState IDSCP_END =	 			new ProtocolState("IDSCP:END", 			"Idscp: end of protocol.");
    public static final ProtocolState IDSCP_RAT_START =			new ProtocolState("IDSCP:RAT:START", 	"Idscp/RemoteAttestation: start of rat sub protocol.");
    public static final ProtocolState IDSCP_RAT_AWAIT_RESPONSE =new ProtocolState("IDSCP:RAT:REPONSE", 	"Idscp/RemoteAttestation: awaiting response.");
    public static final ProtocolState IDSCP_RAT_AWAIT_REQUEST = new ProtocolState("IDSCP:RAT:REQUEST", 	"Idscp/RemoteAttestation: awaiting request.");
    public static final ProtocolState IDSCP_RAT_AWAIT_RESULT =	new ProtocolState("IDSCP:RAT:RESULT", 	"Idscp/RemoteAttestation: awaiting result.");
    public static final ProtocolState IDSCP_RAT_AWAIT_LEAVE =	new ProtocolState("IDSCP:RAT:LEAVE", 	"Idscp/RemoteAttestation: awaiting to leave.");
    public static final ProtocolState IDSCP_META_REQUEST =      new ProtocolState("IDSCP:META:REQUEST", "Idscp/Metadataexchange: request.");
    public static final ProtocolState IDSCP_META_RESPONSE =     new ProtocolState("IDSCP:META:RESPONSE","Idscp/Metadataexchange: response.");
    /* -------------- States end here ------------ */

	private ProtocolState(String id, String description) {
		this.id = id;
		this.description = description;
		this.ord = upperBound++;
	}
	
	public String id() {
		return this.id;
	}
	
	public String description() {
		return this.description;
	}
	
	public static int size() {
		return upperBound;
	}
	
	public String toString() {
		return this.description() + " (id:"+this.id()+")";
	}    
  }