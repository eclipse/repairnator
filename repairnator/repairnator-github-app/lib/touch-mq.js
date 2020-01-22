var container = require('rhea');

module.exports = (buildId) => {
    // https://github.com/eclipse/repairnator/blob/master/repairnator/kubernetes-support/queue-for-buildids/publisher.py
    var args = {
        'username': 'admin',
        'password': 'admin',
        'host': 'localhost',
        'port': 5672, // 61613
        'queue': 'queue://pipeline'
    };

    container.options.username = args.username;
    container.options.password = args.password;

    var connection = container.connect({
        host: args.host,
        port: args.port
    });
    var sender = connection.open_sender(args.queue);

    container.on('connection_open', function (context) {
        console.log('authenticated!');
    });
    container.on('sendable', function (context) {
        console.log('sent ' + buildId + '!');
        // if send plain text, when buildId = 999, then AciveMQ will get: 999
        // context.sender.send({ body: buildId })
        // if send raw binary, when buildId = 999, then ActiveMQ will get: {"buildId":"999"}
        var amqp_message = container.message;
        const stringifiedPayload = JSON.stringify({ 'buildId': buildId });
        const buffer = new Buffer.from(stringifiedPayload, 'utf8');
        const body = amqp_message.data_section(buffer);
        context.sender.send({ body });
    });
    container.on('accepted', function (context) {
        console.log('accepted!');
        context.connection.close();
        console.log('disconnected!');
    });
}