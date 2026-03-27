import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { StyleAction } from '.';
import { of, map, switchMap } from 'rxjs';
import { INITIAL_STATE } from './state';

@Injectable()
export class StyleEffect {
  private readonly actions$ = inject(Actions);

  getStyle$ = createEffect(() =>
    this.actions$.pipe(
      ofType(StyleAction.GetStyle),
      switchMap(() => {
        return of(INITIAL_STATE).pipe(
          map((data) => StyleAction.Update({ newState: data }))
        );
      })
    )
  );
}
