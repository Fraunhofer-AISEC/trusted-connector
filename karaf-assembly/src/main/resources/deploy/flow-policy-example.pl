rule(dropAll).
rule_priority(dropAll,0).
has_decision(dropAll,drop).
receives_label(dropAll,any).
has_target(dropAll,serviceAll).

rule(allowAll).
rule_priority(allowAll,1).
has_decision(allowAll,allow).
receives_label(allowAll,any).
has_target(allowAll,serviceURI).
has_target(allowAll,serviceLog).
has_target(allowAll,serviceCall).

service(serviceURI).
has_endpoint(serviceURI,'^[a-z]+://.*').
creates_label(serviceURI,[]).
removes_label(serviceURI,[]).

service(serviceLog).
has_endpoint(serviceLog,'^log$').
creates_label(serviceLog,[]).
removes_label(serviceLog,[]).

service(serviceCall).
has_endpoint(serviceCall,'^[a-zA-Z]+\[.*\]$').
creates_label(serviceCall,[]).
removes_label(serviceCall,[]).

service(serviceAll).
has_endpoint(serviceAll,'.*').
creates_label(serviceAll,[]).
removes_label(serviceAll,[]).