The _javax-xml-bind-patch_ module provides the javax.xml.bind* packages
that are not available anymore in JDK 11.
In contrast to inclusion via "providedByBundle", this module provides the packages
in the context of the ids feature so integration test can also use them.