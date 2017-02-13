package de.fhg.ids.comm.ws.protocol;

public final class ProtocolState {
	private String id;
	private String description;
	public final int ord;
	private static int upperBound = 0;
	
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

    public static final ProtocolState IDSCP_START = 			new ProtocolState("IDSCP:START", 		"Idscp: start of protocol.");
    public static final ProtocolState IDSCP_ERROR = 			new ProtocolState("IDSCP:ERROR", 		"Idscp: error in protocol.");
    public static final ProtocolState IDSCP_END =	 			new ProtocolState("IDSCP:END", 			"Idscp: end of protocol.");
    public static final ProtocolState IDSCP_RAT_START =			new ProtocolState("IDSCP:RAT:START", 	"Idscp: Rat: start of rat sub protocol.");
    public static final ProtocolState IDSCP_RAT_SUCCESS =		new ProtocolState("IDSCP:RAT:SUCCESS", 	"Idscp: Rat: successful execution of rat sub protocol.");
    public static final ProtocolState IDSCP_RAT_AWAIT_RESPONSE =new ProtocolState("IDSCP:RAT:REPONSE", 	"Idscp: Rat: awaiting response.");
    public static final ProtocolState IDSCP_RAT_AWAIT_REQUEST = new ProtocolState("IDSCP:RAT:REQUEST", 	"Idscp: Rat: awaiting request.");
    public static final ProtocolState IDSCP_RAT_AWAIT_RESULT =	new ProtocolState("IDSCP:RAT:RESULT", 	"Idscp: Rat: awaiting result.");
    public static final ProtocolState IDSCP_RAT_AWAIT_LEAVE =	new ProtocolState("IDSCP:RAT:LEAVE", 	"Idscp: Rat: awaiting to leave.");
    public static final ProtocolState IDSCP_META_AWAIT_REQUEST =new ProtocolState("IDSCP:META:REQUEST", "Idscp: Metadataexchange: await request.");
    
  }