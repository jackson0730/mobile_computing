from django.db import models

# Create your models here.
class User(models.Model):
    ID = models.AutoField(primary_key=True)
    username = models.CharField(max_length=20)
    password = models.CharField(max_length=20)

class Lecture(models.Model):
    ID = models.AutoField(primary_key=True)
    latitude = models.DecimalField(max_digits=9, decimal_places=6)
    longitude =  models.DecimalField(max_digits=9, decimal_places=6)
    dateTime = models.DateTimeField()

class Attendance(models.Model):
    ID = models.AutoField(primary_key=True)
    userID = models.ForeignKey(User, on_delete=models.CASCADE)
    lectureID = models.ForeignKey(Lecture, on_delete=models.CASCADE)