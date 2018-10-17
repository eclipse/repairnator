function getApiUri(path){
  const apiHost = 'http://51.38.235.157:4040/repairnator-mongo-api';
  return `${apiHost}${path}`
}

function apiGet(path, callback){
  console.log(`GET ${path}`)
  return $.ajax({
      type: 'GET',
      headers: {},
      url: getApiUri(path)
  }).done(callback);
}
