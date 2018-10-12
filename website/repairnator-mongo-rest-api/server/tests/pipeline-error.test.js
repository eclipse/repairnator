import request from 'supertest-as-promised';
import httpStatus from 'http-status';
import chai, { expect } from 'chai';
import app from '../../index';

chai.config.includeStack = true;

describe('## Piperline-Erros', () => {
  describe('# GET /repairnator-mongo-api/pipeline-errrors/', () => {
    it('should return OK', (done) => {
      request(app)
        .get('/repairnator-mongo-api/pipeline-errors')
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          done();
        })
        .catch(done);
    });
  });
});
