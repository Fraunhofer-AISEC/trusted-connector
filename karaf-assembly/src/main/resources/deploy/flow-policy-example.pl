rule(allowAll).
has_decision(allowAll,drop).
receives_label(allowAll,[forbidden]).
has_target(allowAll,serviceAll).

service(serviceAll).
has_endpoint(serviceAll,'.*').
creates_label(serviceAll,[]).
removes_label(serviceAll,[]).