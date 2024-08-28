package com.ericsson.oss.services.security.npam.ejb.log;///*
// *******************************************************************************
// * COPYRIGHT Ericsson 2021
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// */
//package com.ericsson.oss.services.nodepam.ejb.log;
//
//import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
//import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
//import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
//import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;
//import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
//import org.slf4j.Logger;
//
//import javax.inject.Inject;
//import java.util.Set;
//
///**
// * Logs commands using the {@link SystemRecorder}.
// */
//public class CmCommandLogger {
//
//    @Inject
//    private Logger logger;
//
//    @Inject
//    private SystemRecorder systemRecorder;
//
//    public static final String CM_REST_NBI = "CM-RestNbi";
//    private static final String CM_CLI_REQUEST_ID = "CM-CLI-Request-ID: ";
//
//    public void logCommandRequest(final NbiRequest nbiRequest) {
//        systemRecorder.recordCommand("nbiRequest=" + nbiRequest.getRecordingInfo(), CommandPhase.STARTED, CM_REST_NBI, "", CM_CLI_REQUEST_ID + nbiRequest.getRequestId());
//    }
//
//    public void logCommandResponse(final String requestId, final NbiResponse nbiResponse, final Set successHttpCodes) {
//        if (successHttpCodes.contains(nbiResponse.getHttpCode())) {
//            systemRecorder.recordCommand("nbiRequest ...", CommandPhase.FINISHED_WITH_SUCCESS, CM_REST_NBI, "",
//                    CM_CLI_REQUEST_ID + requestId + ". httpCode: " + nbiResponse.getHttpCode());
//        } else {
//            systemRecorder.recordError("nbiRequest ...", ErrorSeverity.ERROR, CM_REST_NBI, "",
//                    CM_CLI_REQUEST_ID + requestId + ". httpCode: " + nbiResponse.getHttpCode());
//        }
//    }
//}
