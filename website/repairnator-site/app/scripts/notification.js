function notify(body) {

  if (!('Notification' in window)) {
    console.log('This browser does not support desktop notification');
  }
  else if (Notification.permission === 'granted')
  {
    var options = {
      body,
      icon: './apple-touch-icon.png'
    };
    var notification = new Notification('Repairnator',options);
  }
  else if (Notification.permission !== 'denied')
  {
    Notification.requestPermission(function (permission) {});
  }

}
