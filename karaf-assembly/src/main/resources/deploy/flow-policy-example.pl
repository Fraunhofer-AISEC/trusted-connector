rule(allowAll).
rule_priority(allowAll,1).
has_decision(allowAll,allow).
receives_label(allowAll).
has_target(allowAll,serviceAll).

service(serviceURI).
has_endpoint(serviceURI,'^[a-z]+://.*').

service(serviceLog).
has_endpoint(serviceLog,'^log$').

service(serviceCall).
has_endpoint(serviceCall,'^[a-zA-Z]+\[.*\]$').

service(serviceAll).
has_endpoint(serviceAll,'.*').
