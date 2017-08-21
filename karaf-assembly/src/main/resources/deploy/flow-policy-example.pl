regex(A,B,C) :- class("java.util.regex.Pattern") <- matches(A,B) returns C.
rule(deleteAfterOneMonth).
has_target(deleteAfterOneMonth, service1757980504).
service(service1757980504).
has_endpoint(service1757980504,"hdfs").
receives_label(deleteAfterOneMonth,private).
has_obligation(deleteAfterOneMonth, obl1675366108).
requires_prerequisite(obl1675366108, (delete_after_days(30))).
has_alternativedecision(obl1675366108, drop).
rule(anotherRule).
has_target(anotherRule, hiveMqttBroker). 
receives_label(anotherRule,private).
has_decision(anotherRule, drop).
rule(what).
has_target(what, testQueue). 
receives_label(what,public_data).
has_decision(what, allow).
service(anonymizer).
has_endpoint(anonymizer, ".*anonymizer.*").
creates_label(anonymizer, anonymized).
removes_label(anonymizer, personal).
removes_label(anonymizer, privates).
has_property(anonymizer,myProp,anonymize('surname', 'name')).
service(hiveMqttBroker).
has_endpoint(hiveMqttBroker, "^paho:.*?tcp://broker.hivemq.com:1883.*").
creates_label(hiveMqttBroker, public_data).
creates_label(hiveMqttBroker, sensor_data).
has_property(hiveMqttBroker,type,public).
service(allMqttBrokers).
has_endpoint(allMqttBrokers, "^paho:.*").
creates_label(allMqttBrokers, private_data).
creates_label(allMqttBrokers, sensor_data).
has_property(allMqttBrokers,type,private).
service(testQueue).
has_endpoint(testQueue, "^amqp:.*?:test").
service(idsEndpoints).
has_endpoint(idsEndpoints, "^ids://.*").
creates_label(idsEndpoints, private_data).
service(hadoopCluster).
has_endpoint(hadoopCluster, "hdfs://.*").
has_capability(hadoopCluster,deletion).
has_property(hadoopCluster,anonymizes,anonymize('surname', 'name')).
