#aws-codepipeline-transition-disabler

This package provides a single Java class that can be used as an AWS Lambda function to automatically close the Prod Inbound transition on multiple AWS CodePipelines. This currently only works for Elastic Beanstalk deployments.

# Setup

## Beanstalk Environment

Update the notification settings on your Beanstalk environment:

1. Make sure that your beanstalk environment has notifications enabled by clicking on the "Configuration" page and selecting the "Notifications" tab. Enter an email address and click apply.

## Lambda Function

Create a new Lambda function to run the disabler code:

1. Visit https://console.aws.amazon.com/lambda/home
2. If this will be your first Lambda function, click "Get Started", otherwise click "Create a Lambda Function".
3. Select "Blank Function".
4. On the "Select Triggers" page, click next (we will setup the triggers later).
5. Give your function a name (e.g. pipeline-transition-disabler) and select "Java 8" as the runtime.
6. Build the package locally using `maven clean install` and upload the resulting JAR in the "Function Package" section.
7. Add environment variables for each of the pipelines you want to interact with. For each pipeline, set the key to be the name of the Elastic Beanstalk environment (NOTE: the environment variables do not support `-`, so any `-`'s that appear in your environment name should be replaced with `_` in the key name). Set the value for each key to the name of the pipeline that it should disable (use of `-` is allowed in the values). You can always add more environment variables later, as well.
8. Set the "Handler" to `com.replyyes.lambda.PipelineTransitionDisabler`.
9. Set the "Role" to "Create a custom role".
10. Give the role a "Name" (e.g. lambda_pipline) and click "Next".
11. When you are returned to the Lambda function creation wizard, click "Advanced settings".
12. Optional: decrease the "Memory" to "192MB". This is the minimum memory required to run this function.
13. Optional: increase the "Timeout" to "1 minute". This may help reduce impact from network latency.
14. Click "Next" to view the "Review" screen.
15. Click "Create function" to create the function.

## Configure Triggers

Configure your Lambda function to listen to deployment events:

1. Now that your function has been created, click on the "Triggers" tab.
2. Click "Add Trigger".
3. Click inside the dashed, grey box.
4. Select "SNS".
5. In the "SNS topic" section, select the SNS topic for the environment you wish to add (e.g. ElasticBeanstalkNotifications-Environment-MyEnvironment).
6. Ensure that "Enable trigger" is selected.
7. Click "Submit".
8. Repeat steps 2-7 for each of your pipelines. You can always add new triggers later, as well.

## Configure IAM Role

Provide permissions to the IAM role you created:

1. Visit https://console.aws.amazon.com/iam/home
2. Select "Roles" from the left-hand menu.
3. Click on the role you created above.
4. Click "Create Role Policy" in the "Inline Policies" section.
5. Select "Policy Generator" and click "Select".
6. Select "Allow" as the "Effect".
7. Select "AWS CodePipeline" as the "AWS Service".
8. Select "Disable Stage Transition" as the "Actions".
9. Set "Amazon Resource Name (ARN)" to `*` to allow access to all of your pipelines.
10. Click "Add Statement".
11. Click "Apply Policy".