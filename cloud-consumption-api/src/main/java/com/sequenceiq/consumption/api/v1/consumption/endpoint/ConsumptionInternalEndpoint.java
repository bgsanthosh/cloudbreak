package com.sequenceiq.consumption.api.v1.consumption.endpoint;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionScheduleRequest;
import com.sequenceiq.consumption.api.doc.ConsumptionOpDescription;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionUnscheduleRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/internal/consumption")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/internal/consumption", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ConsumptionInternalEndpoint {

    @POST
    @Path("schedule/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConsumptionOpDescription.SCHEDULE_STORAGE, produces = MediaType.APPLICATION_JSON, nickname = "scheduleStorageCollection")
    void scheduleStorageConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @Valid @NotNull StorageConsumptionScheduleRequest request);

    @DELETE
    @Path("unschedule/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConsumptionOpDescription.UNSCHEDULE_STORAGE, produces = MediaType.APPLICATION_JSON, nickname = "unscheduleStorageCollection")
    void unscheduleStorageConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @Valid @NotNull StorageConsumptionUnscheduleRequest request);
}
