function getApiUri(path){
  const { apiHost, apiPort } = config;
  return `${apiHost}:${apiPort}/repairnator-mongo-api${path}`
}

function apiGet(path, callback){
  console.log(`GET ${path}`)
  return $.ajax({
      type: 'GET',
      headers: {},
      url: getApiUri(path)
  }).done(callback);
}
