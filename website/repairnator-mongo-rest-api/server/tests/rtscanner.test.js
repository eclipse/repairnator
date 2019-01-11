import request from 'supertest-as-promised';
import httpStatus from 'http-status';
import chai, { expect } from 'chai';
import app from '../../index';

chai.config.includeStack = true;

describe('## Piperline-Erros', () => {
  describe('# GET /repairnator-mongo-api/rtscanners/', () => {
    it('should return OK', (done) => {
      request(app)
        .get('/repairnator-mongo-api/rtscanners/')
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          done();
        })
        .catch(done);
    });
  });
  describe('# GET /repairnator-mongo-api/rtscanners/speedrate', () => {
    it('should get the speedrate in the past 24 hours', (done) => {
      request(app)
        .get('/repairnator-mongo-api/inspectors/speedrate')
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          expect(res.body).to.have.lengthOf(24);
          done();
        })
        .catch(done);
    });
  });
});
