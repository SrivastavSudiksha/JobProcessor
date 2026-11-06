from locust import HttpUser, task, between
import random
import json

class JobUser(HttpUser):
    wait_time = between(0.5, 2)

    job_types = [
        {
            "type": "resize_image",
            "payload": {
                "url": "https://example.com/image.jpg",
                "width": 800,
                "height": 600
            }
        },
        {
            "type": "send_email",
            "payload": {
                "to": "loadtest@example.com",
                "subject": "Load test",
                "body": "Hello from Locust"
            }
        },
        {
            "type": "generate_report",
            "payload": {
                "reportType": "daily"
            }
        }
    ]

    @task(5)
    def submit_job(self):
        body = random.choice(self.job_types)
        self.client.post("/api/v1/jobs", json=body)

    @task(2)
    def list_jobs(self):
        self.client.get("/api/v1/jobs?size=10")

    @task(1)
    def health(self):
        self.client.get("/actuator/health")
