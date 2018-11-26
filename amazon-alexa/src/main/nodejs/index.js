/* eslint-disable  func-names */
/* eslint-disable  no-console */

const Alexa = require('ask-sdk-core');
const http = require('http');

const ActivateSceneHandler = {
    canHandle(handlerInput) {
        return handlerInput.requestEnvelope.request.type === 'IntentRequest'
            && handlerInput.requestEnvelope.request.intent.name === 'ActivateScene';
    },
    async handle(handlerInput) {
        let sceneName = handlerInput.requestEnvelope.request.intent.slots.sceneName.value;
        var result = await callHomeApi(sceneName);
        return handlerInput.responseBuilder
            .speak(result.messageContent)
            .getResponse();
    }
};


async function callHomeApi(sceneToActivate, returnCallable) {

    // Build the message
    const activationMessage = {
        type: "VoiceActivation",
        controlIdentifiers: {
            controllerIdentifier: "general",
            nodeIdentifier : "Scenes"
        },
        voiceParameters: {
            sceneName: sceneToActivate
        },
        broadcast: true
    };

    // Call the api and wait for the result

    // Return the result message
    var returnMessage = await postApi(activationMessage,returnCallable);
    console.log("returned message: "+JSON.stringify(returnMessage));
    return returnMessage;

}

function postApi(postBodyObject, returnCallable) {
    return new Promise(resolve => {

        const YOURHOME_API = process.env.YOURHOME_API;
    console.log("Calling api "+ YOURHOME_API +" to activate scene "+postBodyObject.voiceParameters.sceneName);
    var options = {
        "method": "POST",
        "hostname": YOURHOME_API,
        "path": "/api/messagehandler",
        "headers": {
            "Content-Type": "application/json"
        }
    };

    var req = http.request(options, function(res) {
        var chunks = [];
        res.on("data", function(chunk) {
            chunks.push(chunk);
        });

        res.on("end", function() {
            var body = Buffer.concat(chunks);
            resolve(JSON.parse(body.toString()));
        });
    });
    req.write(JSON.stringify(postBodyObject));
    req.end();
});
}

const ErrorHandler = {
    canHandle() {
        return true;
    },
    handle(handlerInput, error) {
        console.log(`Error handled: ${error.message}`);

        return handlerInput.responseBuilder
            .speak('Sorry, an error occurred.')
            .reprompt('Sorry, an error occurred.')
            .getResponse();
    },
};

const skillBuilder = Alexa.SkillBuilders.custom();

exports.handler = skillBuilder
    .addRequestHandlers(
        ActivateSceneHandler
    )
    .addErrorHandlers(ErrorHandler)
    .lambda();
