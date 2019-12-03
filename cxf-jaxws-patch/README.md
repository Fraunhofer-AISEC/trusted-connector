The _cxf-jaxws-patch_ module "injects" the recent version of javax.xml.ws* packages into the bundle
org.apache.cxf.cxf-rt-frontend-jaxws to prevent the bundle from crashing due to class javax.xml.ws.WebServiceFeature
missing in the respective package in Apache Felix. 
