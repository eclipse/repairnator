import request from 'supertest-as-promised';
import httpStatus from 'http-status';
import chai, { expect } from 'chai';
import app from '../../index';

chai.config.includeStack = true;

describe('## Inspector', () => {
  describe('# GET /repairnator-mongo-api/inspectors', () => {
    it('should return OK', (done) => {
      request(app)
        .get('/repairnator-mongo-api/inspectors')
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          done();
        })
        .catch(done);
    });
  });
  describe('# GET /repairnator-mongo-api/inspectors/search', () => {
    it('should search by status', (done) => {
      const status = 'NOTBUILDABLE';
      request(app)
        .get(`/repairnator-mongo-api/inspectors/search?status=${status}`)
        .expect(httpStatus.OK)
        .then((res) => {
          expect(res.body).to.be.an('array');
          const allEqualToStatus = res.body.reduce((acc, elem) =>
            elem.status === status && acc
          , true);
          expect(allEqualToStatus).to.equal(true);
          done();
        })
        .catch(done);
    });
  });
});
