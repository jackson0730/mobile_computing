# Generated by Django 2.2.6 on 2019-10-23 11:58

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Lecture',
            fields=[
                ('ID', models.AutoField(primary_key=True, serialize=False)),
                ('latitude', models.DecimalField(decimal_places=6, max_digits=9)),
                ('longitude', models.DecimalField(decimal_places=6, max_digits=9)),
                ('dateTime', models.DateTimeField()),
                ('alink', models.CharField(max_length=100, null=True)),
            ],
        ),
        migrations.CreateModel(
            name='PictureRequest',
            fields=[
                ('ID', models.IntegerField(primary_key=True, serialize=False)),
                ('data', models.TextField(null=True)),
                ('status', models.CharField(max_length=10)),
                ('lectureID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.Lecture')),
            ],
        ),
        migrations.CreateModel(
            name='User',
            fields=[
                ('ID', models.AutoField(primary_key=True, serialize=False)),
                ('username', models.CharField(max_length=20, unique=True)),
                ('password', models.CharField(max_length=20)),
            ],
        ),
        migrations.CreateModel(
            name='RjectedPictureRequest',
            fields=[
                ('ID', models.IntegerField(primary_key=True, serialize=False)),
                ('lectureID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.Lecture')),
                ('requestID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.PictureRequest')),
                ('userID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.User')),
            ],
        ),
        migrations.CreateModel(
            name='QuestionRequest',
            fields=[
                ('ID', models.IntegerField(primary_key=True, serialize=False)),
                ('data', models.TextField(null=True)),
                ('lectureID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.Lecture')),
                ('userID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.User')),
            ],
        ),
        migrations.AddField(
            model_name='picturerequest',
            name='userID',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.User'),
        ),
        migrations.CreateModel(
            name='Link',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('alink', models.CharField(max_length=100, null=True)),
                ('lectureID', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, to='webapp.Lecture')),
            ],
        ),
        migrations.CreateModel(
            name='ChosenStudent',
            fields=[
                ('ID', models.IntegerField(primary_key=True, serialize=False)),
                ('lectureID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.Lecture')),
                ('userID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.User')),
            ],
        ),
        migrations.CreateModel(
            name='Attendance',
            fields=[
                ('ID', models.IntegerField(primary_key=True, serialize=False)),
                ('lectureID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.Lecture')),
                ('userID', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='webapp.User')),
            ],
        ),
    ]
