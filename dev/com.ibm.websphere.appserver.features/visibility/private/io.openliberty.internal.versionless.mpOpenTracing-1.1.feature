-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpOpenTracing-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-1.4; ibm.tolerates:="2.0", \
    com.ibm.websphere.appserver.mpOpenTracing-1.1
kind=noship
edition=full
