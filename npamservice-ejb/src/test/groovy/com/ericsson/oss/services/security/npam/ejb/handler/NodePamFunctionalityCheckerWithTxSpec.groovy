package com.ericsson.oss.services.security.npam.ejb.handler

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.dao.DpsReadOperations
import com.ericsson.oss.services.security.npam.ejb.exceptions.ExceptionFactory
import com.ericsson.oss.services.security.npam.ejb.exceptions.NodePamError
import com.ericsson.oss.services.security.npam.ejb.executor.ManagedObjectInfo
import spock.lang.Unroll

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_MO_NAMESPACE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_MO_NAMESPACE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MAINTENANCE_USER_SUBJECT_NAME
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETYPE
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NODEROOTREF
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.USER_IDENTITY_MO

/*
This class test single methods (corner cases)
* */
class NodePamFunctionalityCheckerWithTxSpec extends BaseSetupForTestSpecs {

    @ObjectUnderTest
    private NodePamFunctionalityCheckerWithTx objUnderTest

    @MockedImplementation
    DpsReadOperations dpsReadOperationsMock;

    ManagedObject managedObjectMock = Mock(ManagedObject)

    @SpyImplementation    //this annotation allow to spy real object exection
    ExceptionFactory exceptionFactory

    @ImplementationClasses
    def classes = [
    ]

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    def DataBucket dataBucketMock = Mock(DataBucket)

    def 'validateNodeRootFdn with no node throws exception'() {
        given:
            dpsReadOperationsMock.findManagedObject(*_) >> null
        when:
            objUnderTest.validateNodeRootFdn("node1", dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.FDN_NOT_FOUND, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'validateNodeRootFdn with ERBS node throws exception'() {
        given:
            dpsReadOperationsMock.findManagedObject(*_) >> managedObjectMock
            managedObjectMock.getAttribute(NETYPE) >> "ERBS"
        when:
            objUnderTest.validateNodeRootFdn("node1", dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.UNSUPPORTED_NE_TYPE, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'validateNodeRootFdn with node with invalid nodeRootRef throws exception'() {
        given:
            dpsReadOperationsMock.findManagedObject(*_) >> managedObjectMock
            managedObjectMock.getAttribute(NETYPE) >> "RadioNode"
            managedObjectMock.getAssociations(NODEROOTREF)  >> null
        when:
            objUnderTest.validateNodeRootFdn("node1", dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.INVALID_NODE_ROOT, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'getNodeRootRef with empty nodeRootRef return null'() {
        given:
            managedObjectMock.getAssociations(NODEROOTREF)  >> new ArrayList<PersistenceObject>()
        when:
           def associatedNodeRootFdn = objUnderTest.getNodeRootRef(managedObjectMock)
        then:
            associatedNodeRootFdn == null
    }

    def 'validateMaintenanceUserSecurityFdn with 0 MaintenanceUserSecurity instances throws exception'() {
        given: '0 MaintenanceUserSecurity instances'
            def associatedNodeRootFdn = 'ManagedElement=RadioNode1'
            dpsReadOperationsMock.getFdnsGivenBaseFdn(dataBucketMock, MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn) >> {new ArrayList<ManagedObjectInfo>()}
        when:
            objUnderTest.validateMaintenanceUserSecurityFdn(associatedNodeRootFdn, dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.NO_ENTRIES, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'validateMaintenanceUserSecurityFdn with 1 MaintenanceUserSecurity instance not found in DB throws exception'() {
        given: '1 MaintenanceUserSecurity instance not found in DB'
            def associatedNodeRootFdn = 'ManagedElement=RadioNode1'
            List<ManagedObjectInfo> maintenanceUserSecurityManagedObjectInfos = new ArrayList<>()
            maintenanceUserSecurityManagedObjectInfos.add(new ManagedObjectInfo("MaintenanceUserSecurity=1", "MaintenanceUser", MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO))
            dpsReadOperationsMock.getFdnsGivenBaseFdn(dataBucketMock, MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn) >> maintenanceUserSecurityManagedObjectInfos
            dpsReadOperationsMock.findManagedObject("MaintenanceUserSecurity=1", dataBucketMock) >> null
        when:
            objUnderTest.validateMaintenanceUserSecurityFdn(associatedNodeRootFdn, dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.FDN_NOT_FOUND, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'validateMaintenanceUserSecurityFdn with 2 MaintenanceUserSecurity instances throws exception'() {
        given: '2 MaintenanceUserSecurity instances'
           def associatedNodeRootFdn = 'ManagedElement=RadioNode1'
            List<ManagedObjectInfo> maintenanceUserSecurityManagedObjectInfos = new ArrayList<>()
            maintenanceUserSecurityManagedObjectInfos.add(new ManagedObjectInfo("MaintenanceUserSecurity=1", "MaintenanceUser", MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO))
            maintenanceUserSecurityManagedObjectInfos.add(new ManagedObjectInfo("MaintenanceUserSecurity=2", "MaintenanceUser", MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO))
            dpsReadOperationsMock.getFdnsGivenBaseFdn(dataBucketMock, MAINTENANCE_USER_SECURITY_MO_NAMESPACE, MAINTENANCE_USER_SECURITY_MO, associatedNodeRootFdn) >> maintenanceUserSecurityManagedObjectInfos
        when:
            objUnderTest.validateMaintenanceUserSecurityFdn(associatedNodeRootFdn, dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.TOO_MANY_ENTRIES, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'validateUserIdentityFdn with 0 UserIdentity instances throws exception'() {
        given: '0 UserIdentity instances'
            def associatedNodeRootFdn = 'ManagedElement=RadioNode1'
            dpsReadOperationsMock.getFdnsGivenBaseFdn(dataBucketMock, MAINTENANCE_USER_MO_NAMESPACE, USER_IDENTITY_MO, associatedNodeRootFdn) >> {new ArrayList<ManagedObjectInfo>()}
        when:
            objUnderTest.validateUserIdentityFdn(associatedNodeRootFdn, dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.NO_ENTRIES, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'validateUserIdentityFdn with 2 UserIdentity instances throws exception'() {
        given: '2 UserIdentity instances'
            def associatedNodeRootFdn = 'ManagedElement=RadioNode1'
            List<ManagedObjectInfo> userIdentityManagedObjectInfos = new ArrayList<>()
            userIdentityManagedObjectInfos.add(new ManagedObjectInfo("UserIdentity=1", USER_IDENTITY_MO, MAINTENANCE_USER_MO_NAMESPACE, "6.2.2"))
            userIdentityManagedObjectInfos.add(new ManagedObjectInfo("UserIdentity=2", USER_IDENTITY_MO , MAINTENANCE_USER_MO_NAMESPACE, "6.2.2"))
            dpsReadOperationsMock.getFdnsGivenBaseFdn(dataBucketMock, MAINTENANCE_USER_MO_NAMESPACE, USER_IDENTITY_MO, associatedNodeRootFdn) >> userIdentityManagedObjectInfos
        when:
            objUnderTest.validateUserIdentityFdn(associatedNodeRootFdn, dataBucketMock)
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.TOO_MANY_ENTRIES, _)
        and: 'raise exception'
            thrown(Exception)
    }

    def 'fetchRemoteManagementValue with invalid remoteManagement attribute return null'() {
        given:
               managedObjectMock.getAttribute(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE)  >> {throw new Exception("message")}
        when:
            def value = objUnderTest.fetchRemoteManagementValue(managedObjectMock)
        then:
            value == null
    }

    def 'fetchSubjectNameValue with invalid subjectName attribute return null'() {
        given:
            managedObjectMock.getAttribute(MAINTENANCE_USER_SUBJECT_NAME)  >> {throw new Exception("message")}
        when:
            def value = objUnderTest.fetchSubjectNameValue(managedObjectMock)
        then:
            value == null
    }

    @Unroll
    def 'validateRemoteManagementAttributeValue with null remoteManagement=#remoteManagementValue and  expectedRemoteManagementValue=#expectedRemoteManagementValue throws exception'(remoteManagementValue, expectedRemoteManagementValue) {
        given:
            ManagedObjectInfo maintenanceUserSecurityManagedObjectInfo = new ManagedObjectInfo("MaintenanceUserSecurity=1", MAINTENANCE_USER_SECURITY_MO, MAINTENANCE_USER_SECURITY_MO_NAMESPACE, "6.2.2")
            maintenanceUserSecurityManagedObjectInfo.getAttributes().put(MAINTENANCE_USER_SECURITY_REMOTE_MANAGEMENT_ATTRIBUTE, remoteManagementValue)
        when:
        def value = objUnderTest.validateRemoteManagementAttributeValue(maintenanceUserSecurityManagedObjectInfo, expectedRemoteManagementValue);
        then: 'create exception'
            1 * exceptionFactory.createValidationException(NodePamError.REMOTE_MANAGEMENT_VALUE_MISMATCH, _)
        and:
            thrown(Exception)
        where:
        remoteManagementValue | expectedRemoteManagementValue || _
        null                  | true                          || _
        null                  | false                         || _
        true                  | false                         || _
        false                 | true                          || _
    }
}
