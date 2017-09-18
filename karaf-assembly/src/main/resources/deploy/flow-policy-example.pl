regex(A,B,C) :- class("java.util.regex.Pattern") <- matches(A,B) returns C.
rule(allowAll).
has_target(allowAll, serviceAll).
service(serviceAll).
has_endpoint(serviceAll,".*").
creates_label(serviceAll, any).
removes_label(_,none).
receives_label(allowAll,_).
has_decision(allowAll, allow).
