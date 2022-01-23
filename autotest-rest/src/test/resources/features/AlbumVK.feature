#language:ru
@test2

Функционал: Работа с альбомами VK
  - Создать приватный альбом
  - Добавить фотографию в альбом
  - Сделать фотографию обложкой альбома
  - Прокомментировать фотографию
  - Добавить отметку на фотографии
  - Создать публичный альбом
  - Перенести туда фотографию
  - Удалить первый альбом

  Сценарий: Выполнение запросов для создания приватного альбома и добавления обложки
    * создать запрос
      | method | path               |
      | GET    | photos.createAlbum |
    * добавить query параметры
      | title        | Test Album |
      | description  | Test       |
      | privacy_view | only_me    |
    * отправить запрос
    * статус код 200
    * извлечь данные
      | album_id | $.response.id |
    * создать запрос
      | method | path                   |
      | GET    | photos.getUploadServer |
    * добавить query параметры
      | album_id | ${album_id} |
    * отправить запрос
    * статус код 200
    * извлечь данные
      | upload_url | $.response.upload_url |
    * создать запрос
      | method | url           | body      |
      | POST   | ${upload_url} | photo.jpg |
    * добавить параметры для upload_url
    * добавить header
      | Content-Type | multipart/form-data |
    * отправить запрос
    * статус код 200
    * извлечь данные
      | server      | $.server      |
      | photos_list | $.photos_list |
      | aid         | $.aid         |
      | hash        | $.hash        |
    * создать запрос
      | method | path        |
      | GET    | photos.save |
    * добавить query параметры
      | server      | ${server}      |
      | photos_list | ${photos_list} |
      | album_id    | ${aid}         |
      | hash        | ${hash}        |
      | act         | security       |
    * отправить запрос
    * статус код 200

  #Сценарий: Выполнение запросов для добавления комментария и отметки к фотографии альбома

  #Сценарий: Выполнение запросов для создания публичного альбома, перемещения в него фото и удаления старого альбома