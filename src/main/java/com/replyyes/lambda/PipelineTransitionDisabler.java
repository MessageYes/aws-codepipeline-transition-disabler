package com.replyyes.lambda;

import java.util.Set;

import com.amazonaws.services.codepipeline.AWSCodePipeline;
import com.amazonaws.services.codepipeline.AWSCodePipelineClient;
import com.amazonaws.services.codepipeline.model.DisableStageTransitionRequest;
import com.amazonaws.services.codepipeline.model.StageTransitionType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.google.common.collect.ImmutableSet;

/**
 * Detects when a deployment has been completed and closes the associated pipeline's Prod transition.
 * A mapping of BeanstalkEnviroment -> PipelineName must be supplied as environment variables, with
 * dashes replaced by underscores (e.g. key:my_awesome_environment, value:My-Awesome-Pipeline).
 */
public class PipelineTransitionDisabler implements RequestHandler<SNSEvent, Boolean> {
    protected static final Set<String> DEPLOYMENT_SUBJECTS = ImmutableSet.of(
        "AWS Elastic Beanstalk Notification - New application version was deployed to running EC2 instances.",
        "AWS Elastic Beanstalk Notification - Failed to deploy application.");

    private final AWSCodePipeline awsCodePipeline = new AWSCodePipelineClient();

    @Override
    public Boolean handleRequest(SNSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        for (SNSRecord record : event.getRecords()) {
            SNS payload = record.getSNS();

            // ignore non-deployments
            if (!DEPLOYMENT_SUBJECTS.contains(payload.getSubject())) {
                continue;
            }

            // extract the beanstalk environment
            String environment = payload.getTopicArn().split(":")[5].replace("ElasticBeanstalkNotifications-Environment-", "");
            logger.log("Received deployment event for " + environment);

            // try to get the pipeline name from the system properties
            String pipeline = System.getenv(environment.replaceAll("-", "_"));
            if (pipeline == null) {
                logger.log("No pipeline found");
                continue;
            }

            logger.log("Attempting to disable prod transition for " + pipeline);

            DisableStageTransitionRequest request = new DisableStageTransitionRequest();
            request.setPipelineName(pipeline);
            request.setReason("Manual approval required");
            request.setStageName("Prod");
            request.setTransitionType(StageTransitionType.Inbound);

            awsCodePipeline.disableStageTransition(request);
            logger.log("Transition has been disabled");
        }

        return true;
    }
}
