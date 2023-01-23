#include <FS.h>
#include <SD.h>

const char *fileName = "/schedules.txt";

void merge(File &file, int l, int m, int r)
{
    int i, j, k;
    int n1 = m - l + 1;
    int n2 = r - m;
    String L[n1], R[n2];
    for (i = 0; i < n1; i++)
    {
        L[i] = file.readStringUntil('-');
    }
    for (j = 0; j < n2; j++)
    {
        R[j] = file.readStringUntil('-');
    }
    i = 0;
    j = 0;
    k = l;
    while (i < n1 && j < n2)
    {
        if (L[i].substring(L[i].indexOf(",") + 1).toInt() <= R[j].substring(R[j].indexOf(",") + 1).toInt())
        {
            file.print(L[i]);
            i++;
        }
        else
        {
            file.print(R[j]);
            j++;
        }
        k++;
    }
    while (i < n1)
    {
        file.print(L[i]);
        i++;
        k++;
    }
    while (j < n2)
    {
        file.print(R[j]);
        j++;
        k++;
    }
}

void mergeSort(File &file, int l, int r)
{
    if (l < r)
    {
        int m = l + (r - l) / 2;
        mergeSort(file, l, m);
        mergeSort(file, m + 1, r);
        merge(file, l, m, r);
    }
}

void sortSchedules()
{
    File file = SD.open(fileName);
    if (!file)
    {
        return;
    }
    int n = file.size();
    mergeSort(file, 0, n - 1);
    file.close();
}
