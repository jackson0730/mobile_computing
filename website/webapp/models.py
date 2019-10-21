from django.db import models

class User(models.Model):
    ID = models.AutoField(primary_key=True)
    username = models.CharField(max_length=20)
    password = models.CharField(max_length=20)

class Lecture(models.Model):
    ID = models.AutoField(primary_key=True)
    latitude = models.DecimalField(max_digits=9, decimal_places=6)
    longitude =  models.DecimalField(max_digits=9, decimal_places=6)
    dateTime = models.DateTimeField()
    link = models.CharField(null=True, max_length=100)

class Attendance(models.Model):
    ID = models.AutoField(primary_key=True)
    userID = models.ForeignKey(User, on_delete=models.CASCADE)
    lectureID = models.ForeignKey(Lecture, on_delete=models.CASCADE)

class PictureRequest(models.Model):
    userID = models.OneToOneField(User, on_delete=models.CASCADE)
    data = models.TextField(null=True)
    status = models.CharField(max_length=10) # available/taken/done

class QuestionRequest(models.Model):
    userID = models.OneToOneField(User, on_delete=models.CASCADE)
    data = models.TextField(null=True)

class ChosenStudent(models.Model):
    userID = models.OneToOneField(User, on_delete=models.CASCADE)

class Link(models.Model):
    link = models.CharField(null=True, max_length=100)