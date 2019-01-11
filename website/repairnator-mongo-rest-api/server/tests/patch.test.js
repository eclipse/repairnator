import request from 'supertest-as-promised';
import httpStatus from 'http-status';
import chai, { expect } from 'chai';
import app from '../../index';

chai.config.includeStack = true;

describe('## Patches', () => {
  describe('# GET /repairnator-mongo-api/patches/', () => {
    it('should return OK', (done) => {
      request(app)
        .get('/repairnator-mongo-api/patches/')
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          done();
        })
        .catch(done);
    });
  });
  describe('# GET /repairnator-mongo-api/patches/builds/:buildId', () => {
    it('should return OK', (done) => {
      request(app)
        .get('/repairnator-mongo-api/patches/builds/461420103')
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          done();
        })
        .catch(done);
    });
  });
});
